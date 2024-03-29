/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package com.fnproject.springframework.function;

import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.springframework.function.testfns.EmptyFunctionConfig;
import com.fnproject.springframework.function.testfns.FunctionConfig;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;


public class SpringCloudFunctionInvokerIntegrationTest {

    @Rule
    public final FnTestingRule fnRule = FnTestingRule.createDefault();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void shouldInvokeFunction() {

        fnRule.givenEvent().withBody("HELLO").enqueue();
        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        assertThat(fnRule.getOnlyResult().getBodyAsString()).isEqualTo("hello");
    }

    @Test
    @Ignore("Consumer behaviour seems broken in this release of Spring Cloud Function")
    // NB the problem is that FluxConsumer is not a subclass of j.u.f.Consumer, but _is_
    // a subclass of j.u.f.Function.
    // Effectively a Consumer<T> is treated as a Function<T, Void> which means when we lookup
    // by env var name "consumer", we don't find a j.u.f.Consumer, so we fall back to the default
    // behaviour which is to invoke the bean named "function"
    public void shouldInvokeConsumer() {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_CONSUMER_NAME, "consumer");
        fnRule.givenEvent().withBody("consumer input").enqueue();

        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        assertThat(fnRule.getStdErrAsString()).contains("consumer input");
    }

    @Test
    public void shouldInvokeSupplier() {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_SUPPLIER_NAME, "supplier");
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        String output = fnRule.getOnlyResult().getBodyAsString();
        assertThat(output).isEqualTo("Hello");
    }

    @Test
    public void shouldThrowFunctionLoadExceptionIfNoValidFunction() {
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(EmptyFunctionConfig.class, "handleRequest");

        int exitCode = fnRule.getLastExitCode();

        assertThat(exitCode).isEqualTo(1);
        assertThat(fnRule.getResults()).isEmpty(); // fails at init so no results.
        assertThat(fnRule.getStdErrAsString()).contains("No Spring Cloud Function found");
    }

    @Test
    public void noNPEifFunctionReturnsNull() {
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(EmptyFunctionConfig.class, "handleRequest");

        int exitCode = fnRule.getLastExitCode();

        assertThat(exitCode).isEqualTo(1);
        assertThat(fnRule.getResults()).isEmpty(); // fails at init so no results.
        assertThat(fnRule.getStdErrAsString()).contains("No Spring Cloud Function found");
    }
}

