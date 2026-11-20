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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.exception.FunctionIOException;
import com.fnproject.fn.runtime.http.spi.HttpHandler;
import com.fnproject.fn.runtime.http.spi.HttpHeader;
import com.fnproject.fn.runtime.http.spi.HttpServer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FnHttpHandlerTest {
    @Test
    public void mapsBinaryBodyMetadataAndRepeatedHeaders() {
        byte[] body = {0, 1, -1};
        HttpTestExchange exchange = new HttpTestExchange();
        exchange.input = new ByteArrayInputStream(body);
        exchange.requestHeaders = Arrays.asList(
            new HttpHeader("fn-call-id", "call-id"),
            new HttpHeader("FN-DEADLINE", "2002-10-02T10:00:00.992Z"),
            new HttpHeader("X-Custom", "first"), new HttpHeader("x-custom", "second"),
            new HttpHeader("Host", "localhost"), new HttpHeader("Transfer-Encoding", "chunked"));
        FnHttpHandler handler = new FnHttpHandler(event -> {
            assertThat(event.getCallID()).isEqualTo("call-id");
            assertThat(event.getDeadline().toEpochMilli()).isEqualTo(1033552800992L);
            assertThat(event.getHeaders().getAllValues("X-Custom")).containsExactly("first", "second");
            assertThat(event.getHeaders().get("Host")).isEmpty();
            assertThat(event.getHeaders().get("Transfer-Encoding")).isEmpty();
            byte[] received = event.consumeBody(stream -> {
                try {
                    return IOUtils.toByteArray(stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            assertThat(received).isEqualTo(body);
            return OutputEvent.fromBytes(received, OutputEvent.Status.FunctionError, "application/octet-stream",
                Headers.emptyHeaders().addHeader("X-Reply", "a", "b")
                    .addHeader("Content-Length", "99").addHeader("Fn-Fdk-Version", "spoofed"));
        }, "fdk", "runtime");

        handler.handle(exchange, exchange);

        assertThat(exchange.statusCode).isEqualTo(502);
        assertThat(exchange.reasonPhrase).isEqualTo("FunctionError");
        assertThat(exchange.body).isEqualTo(body);
        assertThat(exchange.responseHeaders.stream().filter(h -> h.getName().equals("X-Reply"))
            .map(HttpHeader::getValue)).containsExactly("a", "b");
        assertThat(exchange.header("Content-Length")).isNull();
        assertThat(exchange.header("Content-Type")).isEqualTo("application/octet-stream");
        assertThat(exchange.header("Fn-Fdk-Version")).isEqualTo("fdk");
        assertThat(exchange.header("Fn-Fdk-Runtime")).isEqualTo("runtime");
    }

    @Test
    public void preservesInputInvocationAndSerializationErrors() throws Exception {
        FnHttpHandler neverInvoke = new FnHttpHandler(event -> {
            throw new AssertionError("invalid input must not invoke the function");
        }, "fdk", "runtime");
        for (String header : Arrays.asList("Fn-Call-Id", "Fn-Deadline")) {
            HttpTestExchange missing = new HttpTestExchange();
            missing.requestHeaders.removeIf(h -> h.getName().equals(header));
            neverInvoke.handle(missing, missing);
            assertError(missing, "Invalid input for function");
        }
        HttpTestExchange invalid = new HttpTestExchange();
        invalid.requestHeaders.set(1, new HttpHeader("Fn-Deadline", "invalid"));
        neverInvoke.handle(invalid, invalid);
        assertError(invalid, "Invalid input for function");

        HttpTestExchange failed = new HttpTestExchange();
        new FnHttpHandler(event -> { throw new IllegalStateException("quoted \"error\""); }, "fdk", "runtime")
            .handle(failed, failed);
        assertThat(assertError(failed, "Unhandled internal error in FDK").get("detail").asText())
            .isEqualTo("quoted \"error\"");

        HttpTestExchange badOutput = new HttpTestExchange();
        new FnHttpHandler(event -> output(out -> { throw new IOException("write failed"); }), "fdk", "runtime")
            .handle(badOutput, badOutput);
        assertError(badOutput, "Unhandled internal error while writing FDK response");
    }

    @Test
    public void keepsLegacyRouteMatching() {
        AtomicInteger calls = new AtomicInteger();
        FnHttpHandler handler = new FnHttpHandler(event -> {
            calls.incrementAndGet();
            return OutputEvent.emptyResult(OutputEvent.Status.Success);
        }, "fdk", "runtime");
        for (String target : Arrays.asList("/call?x=1", "/call#fragment", "/unknown", "/call/extra")) {
            HttpTestExchange exchange = new HttpTestExchange();
            exchange.target = target;
            handler.handle(exchange, exchange);
            assertThat(exchange.statusCode).isEqualTo(target.startsWith("/call?") || target.startsWith("/call#") ? 200 : 501);
        }
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test(timeout = 10000)
    public void serializesFunctionAndOutputAcrossConcurrentRequests() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        FnHttpHandler handler = new FnHttpHandler(event -> {
            int call = calls.incrementAndGet();
            return output(out -> {
                if (call == 1) {
                    writing.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) {
                            throw new IOException("output timed out");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                }
                out.write(call);
            });
        }, "fdk", "runtime");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        HttpTestExchange first = new HttpTestExchange();
        HttpTestExchange second = new HttpTestExchange();
        try {
            Future<?> a = workers.submit(() -> handler.handle(first, first));
            assertThat(writing.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> b = workers.submit(() -> {
                secondStarted.countDown();
                handler.handle(second, second);
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> b.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(calls.get()).isEqualTo(1);
            release.countDown();
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertThat(first.body).containsExactly(1);
            assertThat(second.body).containsExactly(2);
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    public void closesTransportWhenRunFails() {
        AtomicBoolean closed = new AtomicBoolean();
        HttpServer server = new HttpServer() {
            @Override
            public void run(HttpHandler handler) throws IOException {
                throw new IOException("transport failed");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        assertThatThrownBy(() -> new HttpServerCodec(server, "fdk", "runtime").runCodec(event -> null))
            .isInstanceOf(FunctionIOException.class).hasCauseInstanceOf(IOException.class);
        assertThat(closed.get()).isTrue();
    }

    private JsonNode assertError(HttpTestExchange exchange, String message) throws IOException {
        assertThat(exchange.statusCode).isEqualTo(500);
        assertThat(exchange.contentType).isEqualTo("application/json; charset=UTF-8");
        JsonNode body = new ObjectMapper().readTree(exchange.body);
        assertThat(body.get("message").asText()).isEqualTo(message);
        return body;
    }

    private interface Writer {
        void write(OutputStream out) throws IOException;
    }

    private static OutputEvent output(Writer writer) {
        return new OutputEvent() {
            @Override
            public Status getStatus() {
                return Status.Success;
            }

            @Override
            public Headers getHeaders() {
                return Headers.emptyHeaders();
            }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {
                writer.write(out);
            }
        };
    }
}
