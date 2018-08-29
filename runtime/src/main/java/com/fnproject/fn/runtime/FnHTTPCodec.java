package com.fnproject.fn.runtime;


import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

/**
 * Fn HTTP codec
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FnHTTPCodec implements EventCodec,Closeable {


    private final Map<String, String> env;
    private volatile HttpExchange current;
    private final Object lock = new Object();
    private final BlockingQueue<HttpExchange> calls = new ArrayBlockingQueue<>(1);
    private final HttpServer s;

    public FnHTTPCodec(Map<String, String> env) throws IOException {
        this.env = env;
        s = HttpServer.create(new InetSocketAddress(8080), 10);
        s.setExecutor(Executors.newFixedThreadPool(2));
        s.createContext("/call", this::setExchange);
        s.start();
    }

    private void setExchange(HttpExchange exchange) throws IllegalStateException {
        synchronized (lock) {
            if (this.current != null) {
                throw new IllegalStateException("Concurrent function invocation not supported");
            }
            this.current = exchange;
            this.lock.notify();
        }
    }

    private synchronized HttpExchange releaseExchange() {
        synchronized (lock) {
            if (this.current == null) {
                throw new IllegalArgumentException("exchange already cleared");
            }
            HttpExchange exchange = this.current;
            this.current = null;
            return exchange;
        }

    }

    private String getRequiredEnv(String name) {
        String val = env.get(name);
        if (val == null) {
            throw new FunctionInputHandlingException("Required environment variable " + name + " is not set - are you running a function outside of fn run?");
        }
        return val;
    }

    @Override
    public Optional<InputEvent> readEvent() {

        try {
            synchronized (lock) {
                lock.wait();
            }

            HttpExchange e = current;
            if (current == null) {
                throw new IllegalStateException("didn't get ");
            }

            Headers h = e.getRequestHeaders();

            InputStream bodyStream = e.getRequestBody();

            String appName = getRequiredEnv("FN_APP_NAME");
            String route = getRequiredEnv("FN_PATH");
            String method = h.getFirst("fn_method");
            // TODO remove HTTP-specific stuff
            if (method == null) {
                method = "GET";
            }


            String requestUrl = h.getFirst("fn_request_url");
            if (requestUrl == null) {
                requestUrl = "";
            }


            Map<String, String> headers = new HashMap<>();
            h.forEach((k, vs) -> headers.put(k, vs.get(0)));

            ReadOnceInputEvent inputEvent = new ReadOnceInputEvent(appName, route, requestUrl, method, bodyStream, com.fnproject.fn.api.Headers.fromMap(headers), QueryParametersParser.getParams(requestUrl));
            return Optional.of(inputEvent);
        } catch (InterruptedException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public void writeEvent(OutputEvent evt) {
        HttpExchange exchange = releaseExchange();
        Headers outHeaders = exchange.getResponseHeaders();

        evt.getHeaders().getAll().forEach((k, v) -> outHeaders.put(k, Collections.singletonList(v)));

        evt.getContentType().ifPresent(v -> outHeaders.set("Content-Type", v));

        try {
            exchange.sendResponseHeaders(evt.getStatusCode(), 0);

            evt.writeToOutput(exchange.getResponseBody());
        } catch (IOException e) {
            throw new FunctionOutputHandlingException("failed to write response", e);
        } finally {
            exchange.close();
        }

    }

    public void close() {
        s.stop(0);
    }
}
