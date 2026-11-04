/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.testing;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.InvocationListener;
import com.fnproject.fn.api.RuntimeFeature;
import org.junit.Test;

import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FnTestingInstanceTest {
    public interface Storage {
        String upload(String name);
    }

    public static class Payload {
        public String name;
        public Payload() { }
        public Payload(String name) { this.name = name; }
    }

    public static class Feature implements RuntimeFeature {
        public void initialize(RuntimeContext context) {
            context.setAttribute("feature", "ready");
        }
    }

    @FnFeature(Feature.class)
    public static class UploadFunction {
        private final Storage storage;
        int configurations;
        String prefix;
        public UploadFunction(Storage storage) { this.storage = storage; }

        @FnConfiguration
        public void configure(RuntimeContext context) {
            configurations++;
            assertEquals("ready", context.getAttribute("feature", String.class).get());
            prefix = context.getConfigurationByKey("PREFIX").orElse("");
        }

        public Payload handle(Payload input, RuntimeContext context) {
            assertSame(this, context.getInvokeInstance().get());
            assertEquals(prefix, context.getConfigurationByKey("PREFIX").orElse(""));
            assertEquals(1, configurations);
            return new Payload(prefix + storage.upload(input.name));
        }

        public static String staticHandle(String input) { return input; }
    }

    public static class ReconfiguringUploadFunction {
        private Storage storage;
        private final Storage replacement;

        public ReconfiguringUploadFunction(Storage storage, Storage replacement) {
            this.storage = storage;
            this.replacement = replacement;
        }

        @FnConfiguration
        public void configure() {
            storage = replacement;
        }

        public String handle(String name) {
            return storage.upload(name);
        }
    }

    @Test
    public void configurationCanReplaceInjectedDependencyWithoutReinjection() {
        Storage original = mock(Storage.class);
        Storage replacement = mock(Storage.class);
        when(replacement.upload("report.csv")).thenReturn("replacement-result");
        FnTestingRule testing = FnTestingRule.createDefault();
        testing.givenEvent().withBody("report.csv").enqueue();

        testing.thenRunWithInstance(new ReconfiguringUploadFunction(original, replacement), "handle");

        assertEquals(0, testing.getLastExitCode());
        assertEquals("replacement-result", testing.getOnlyResult().getBodyAsString());
        verify(replacement).upload("report.csv");
        verifyNoInteractions(original);
    }

    @Test
    public void invokesInjectedMockWithBindingAndReusesInstance() {
        Storage storage = mock(Storage.class);
        when(storage.upload("report.csv")).thenReturn("uploaded");
        UploadFunction function = new UploadFunction(storage);
        FnTestingRule testing = FnTestingRule.createDefault().setConfig("PREFIX", "test-");
        testing.givenEvent().withBody("{\"name\":\"report.csv\"}").enqueue(2);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        PrintStream out = System.out;
        PrintStream err = System.err;

        testing.thenRunWithInstance(function, "handle");

        assertEquals(0, testing.getLastExitCode());
        assertEquals(2, testing.getResults().size());
        assertEquals("{\"name\":\"test-uploaded\"}", testing.getResults().get(0).getBodyAsString());
        assertEquals("{\"name\":\"test-uploaded\"}", testing.getResults().get(1).getBodyAsString());
        assertEquals(1, function.configurations);
        verify(storage, times(2)).upload("report.csv");
        assertSame(loader, Thread.currentThread().getContextClassLoader());
        assertSame(out, System.out);
        assertSame(err, System.err);
    }

    @Test
    public void capturesDependencyFailureAndRestoresProcessState() {
        Storage storage = mock(Storage.class);
        when(storage.upload("report.csv")).thenThrow(new IllegalStateException("storage unavailable"));
        FnTestingRule testing = FnTestingRule.createDefault();
        testing.givenEvent().withBody("{\"name\":\"report.csv\"}").enqueue();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        PrintStream out = System.out;
        PrintStream err = System.err;

        testing.thenRunWithInstance(new UploadFunction(storage), "handle");

        assertEquals(1, testing.getLastExitCode());
        assertFalse(testing.getOnlyResult().isSuccess());
        assertTrue(testing.getStdErrAsString().contains("storage unavailable"));
        verify(storage).upload("report.csv");
        assertSame(loader, Thread.currentThread().getContextClassLoader());
        assertSame(out, System.out);
        assertSame(err, System.err);
    }

    @Test
    public void independentRulesDoNotShareInstancesOrConfiguration() {
        Storage first = mock(Storage.class);
        Storage second = mock(Storage.class);
        when(first.upload("file")).thenReturn("first");
        when(second.upload("file")).thenReturn("second");
        FnTestingRule one = FnTestingRule.createDefault().setConfig("PREFIX", "one-");
        FnTestingRule two = FnTestingRule.createDefault();
        one.givenEvent().withBody("{\"name\":\"file\"}").enqueue();
        two.givenEvent().withBody("{\"name\":\"file\"}").enqueue();
        one.thenRunWithInstance(new UploadFunction(first), "handle");
        two.thenRunWithInstance(new UploadFunction(second), "handle");
        assertEquals("{\"name\":\"one-first\"}", one.getOnlyResult().getBodyAsString());
        assertEquals("{\"name\":\"second\"}", two.getOnlyResult().getBodyAsString());
        verify(first).upload("file");
        verify(second).upload("file");
    }

    @Test
    public void rejectsInvalidTargetsBeforeExecution() {
        FnTestingRule testing = FnTestingRule.createDefault();
        assertThrows(NullPointerException.class, () -> testing.thenRunWithInstance(null, "handle"));
        UploadFunction function = new UploadFunction(mock(Storage.class));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> testing.thenRunWithInstance(function, "staticHandle")).getMessage().contains("non-static"));
        assertTrue(assertThrows(RuntimeException.class,
                () -> testing.thenRunWithInstance(function, "missing")).getMessage().contains("not found"));
    }

    @Test
    public void rejectsRuleFeaturesWithoutRunningTheirHooks() {
        FnTestingRule testing = FnTestingRule.createDefault();
        FnTestingRuleFeature feature = mock(FnTestingRuleFeature.class);
        testing.addFeature(feature);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> testing.thenRunWithInstance(new UploadFunction(mock(Storage.class)), "handle"))
                .getMessage().contains("FnTestingRuleFeature"));
        verifyNoInteractions(feature);
    }

    public static class BadConfiguration {
        @FnConfiguration
        public void configure() { throw new IllegalStateException("configuration failed"); }
        public String handle(String input) { return input; }
    }

    @Test
    public void configurationFailureUsesNormalInitializationErrorPath() {
        FnTestingRule testing = FnTestingRule.createDefault();
        testing.givenEvent().withBody("input").enqueue();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        PrintStream out = System.out;
        PrintStream err = System.err;
        testing.thenRunWithInstance(new BadConfiguration(), "handle");
        assertEquals(2, testing.getLastExitCode());
        assertFalse(testing.getOnlyResult().isSuccess());
        assertTrue(testing.getStdErrAsString().contains("configuration failed"));
        assertSame(loader, Thread.currentThread().getContextClassLoader());
        assertSame(out, System.out);
        assertSame(err, System.err);
    }
    public static class LifecycleFunction {
        int calls;
        String prefix;

        @FnConfiguration
        public void configure(RuntimeContext context) {
            prefix = context.getConfigurationByKey("PREFIX").orElse("");
        }

        public String handle(String input, InvocationContext context) {
            context.addResponseHeader("test-header", "present");
            context.addListener(new InvocationListener() {
                public void onSuccess() { System.err.println("listener-success"); }
                public void onFailure() { System.err.println("listener-failure"); }
            });
            calls++;
            if (input.equals("fail")) {
                throw new IllegalStateException("expected failure");
            }
            return prefix + input + calls;
        }
    }

    @Test
    public void matchesClassBasedLifecycleForSuccessAndFailure() {
        for (String input : new String[] {"hello", "fail"}) {
            FnTestingRule normal = FnTestingRule.createDefault().setConfig("PREFIX", "test-");
            FnTestingRule supplied = FnTestingRule.createDefault().setConfig("PREFIX", "test-");
            normal.givenEvent().withBody(input).enqueue(2);
            supplied.givenEvent().withBody(input).enqueue(2);
            normal.thenRun(LifecycleFunction.class, "handle");
            supplied.thenRunWithInstance(new LifecycleFunction(), "handle");
            assertEquals(normal.getLastExitCode(), supplied.getLastExitCode());
            assertEquals(normal.getResults().size(), supplied.getResults().size());
            for (int i = 0; i < 2; i++) {
                assertEquals(normal.getResults().get(i).isSuccess(), supplied.getResults().get(i).isSuccess());
                assertEquals(normal.getResults().get(i).getBodyAsString(), supplied.getResults().get(i).getBodyAsString());
                assertEquals(normal.getResults().get(i).getHeaders().asMap(), supplied.getResults().get(i).getHeaders().asMap());
            }
            String callback = input.equals("fail") ? "listener-failure" : "listener-success";
            assertTrue(normal.getStdErrAsString().contains(callback));
            assertTrue(supplied.getStdErrAsString().contains(callback));
        }
    }

}
