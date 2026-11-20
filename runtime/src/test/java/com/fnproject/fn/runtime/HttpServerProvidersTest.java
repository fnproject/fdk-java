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

package com.fnproject.fn.runtime;

import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.exception.FunctionInitializationException;
import com.fnproject.fn.runtime.http.spi.HttpHandler;
import com.fnproject.fn.runtime.http.spi.HttpServer;
import com.fnproject.fn.runtime.http.spi.HttpServerProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpServerProvidersTest {
    private static final String SERVICE = "META-INF/services/" + HttpServerProvider.class.getName();
    private static final String FDK_VERSION = "test-fdk";
    private static final String RUNTIME_VERSION = "test-runtime";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassLoader originalLoader;

    @Before
    public void setUp() {
        originalLoader = Thread.currentThread().getContextClassLoader();
        TestProvider.environment = null;
        TestProvider.exchange = null;
        TestProvider.closed = false;
    }

    @After
    public void restoreClassLoader() {
        Thread.currentThread().setContextClassLoader(originalLoader);
    }

    @Test
    public void noProviderUsesExistingListenerValidation() throws Exception {
        register("");
        Map<String, String> env = Collections.singletonMap("FN_LISTENER", "tcp:/invalid");

        // The legacy constructor validates the listener before loading JNI.
        assertThatThrownBy(() -> create(env))
            .isInstanceOf(FunctionInitializationException.class)
            .hasMessage("Invalid listener address - it should start with unix:/ :'tcp:/invalid'");
    }

    @Test
    public void createsRegisteredProviderWithoutConstructingDefaultCodec() throws Exception {
        register(TestProvider.class.getName());
        Map<String, String> env = new HashMap<>();
        env.put("custom", "value");
        EventCodec codec = create(env);

        codec.runCodec(event -> {
            assertThat(event.getCallID()).isEqualTo("call-id");
            return OutputEvent.fromBytes(new byte[] {0, 1, -1}, OutputEvent.Status.Success,
                "application/octet-stream");
        });
        assertThat(TestProvider.exchange.statusCode).isEqualTo(200);
        assertThat(TestProvider.exchange.body).containsExactly(0, 1, -1);
        assertThat(TestProvider.exchange.header("Fn-Fdk-Version")).isEqualTo(FDK_VERSION);
        assertThat(TestProvider.exchange.header("Fn-Fdk-Runtime")).isEqualTo(RUNTIME_VERSION);
        assertThat(TestProvider.closed).isTrue();
        assertThat(TestProvider.environment).containsEntry("custom", "value");
        env.put("custom", "changed");
        assertThat(TestProvider.environment).containsEntry("custom", "value");
        assertThatThrownBy(() -> TestProvider.environment.put("custom", "changed"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void rejectsMultipleProvidersBeforeCreatingServer() throws Exception {
        register(TestProvider.class.getName() + "\n" + SecondProvider.class.getName());
        assertThatThrownBy(() -> create(Collections.emptyMap()))
            .isInstanceOf(FunctionInitializationException.class)
            .hasMessageContaining("Multiple HttpServerProvider");
        assertThat(TestProvider.environment).isNull();
    }

    @Test
    public void invalidRegistrationDoesNotFallBack() throws Exception {
        register("missing.HttpServerProvider");
        assertThatThrownBy(() -> create(Collections.emptyMap()))
            .isInstanceOf(FunctionInitializationException.class)
            .hasMessage("Unable to load HttpServerProvider")
            .hasCauseInstanceOf(ServiceConfigurationError.class);
    }

    @Test
    public void creationFailureDoesNotFallBack() throws Exception {
        register(FailingProvider.class.getName());
        assertThatThrownBy(() -> create(Collections.emptyMap()))
            .isInstanceOf(FunctionInitializationException.class)
            .hasMessageContaining(FailingProvider.class.getName())
            .hasCauseInstanceOf(IOException.class);
    }

    private EventCodec create(Map<String, String> environment) {
        return HttpServerProviders.create(environment, FDK_VERSION, RUNTIME_VERSION);
    }

    private void register(String contents) throws IOException {
        Path descriptor = temporaryFolder.newFile().toPath();
        Files.write(descriptor, contents.getBytes(StandardCharsets.UTF_8));
        URL resource = descriptor.toUri().toURL();
        // Isolate service registrations without adding a provider to every test's classpath.
        ClassLoader loader = new ClassLoader(HttpServerProvider.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (SERVICE.equals(name)) {
                    return Collections.enumeration(Collections.singleton(resource));
                }
                return super.getResources(name);
            }
        };
        Thread.currentThread().setContextClassLoader(loader);
    }

    public static class TestProvider implements HttpServerProvider {
        static Map<String, String> environment;
        static HttpTestExchange exchange;
        static boolean closed;

        @Override
        public HttpServer create(Map<String, String> env) throws Exception {
            environment = env;
            return new HttpServer() {
                @Override
                public void run(HttpHandler handler) {
                    exchange = new HttpTestExchange();
                    handler.handle(exchange, exchange);
                }

                @Override
                public void close() {
                    closed = true;
                }
            };
        }
    }

    public static class SecondProvider extends TestProvider {
    }

    public static class FailingProvider extends TestProvider {
        @Override
        public HttpServer create(Map<String, String> env) throws IOException {
            throw new IOException("cannot start server");
        }
    }

}
