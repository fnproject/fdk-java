package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Wrapper for an incoming fn invocation event
 * <p>
 * This in
 */
public class ReadOnceInputEvent implements InputEvent {

    private final String appName;
    private final String route;
    private final String requestUrl;
    private final String method;

    private final BufferedInputStream body;
    private final String callId;
    private AtomicBoolean consumed = new AtomicBoolean(false);
    private QueryParameters queryParameters;
    private final Headers headers;


    public ReadOnceInputEvent(String appName, String route, String requestUrl, String method, String callId, InputStream body, Headers headers, QueryParameters parameters) {
        this.appName = Objects.requireNonNull(appName);
        this.route = Objects.requireNonNull(route);
        this.requestUrl = Objects.requireNonNull(requestUrl);
        this.method = Objects.requireNonNull(method).toUpperCase();
        this.body = new BufferedInputStream(Objects.requireNonNull(body));
        this.headers = Objects.requireNonNull(headers);
        this.queryParameters = Objects.requireNonNull(parameters);
        this.callId = Objects.requireNonNull(callId);
        body.mark(Integer.MAX_VALUE);
    }



    @Override
    public <T> T consumeBody(Function<InputStream, T> dest) {
        if (consumed.compareAndSet(false, true)) {
            try (InputStream rb = body) {
                return dest.apply(rb);
            } catch (IOException e) {
                throw new FunctionInputHandlingException("Error reading input stream", e);
            }
        } else {
            throw new IllegalStateException("Body has already been consumed");
        }

    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getPath    () {
        return route;
    }

    @Override
    public String getRequestUrl() {
        return requestUrl;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getCallId() {
        return callId;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }


    @Override
    public QueryParameters getQueryParameters() {
        return queryParameters;
    }

    @Override
    public void close() throws IOException {
        body.close();
    }
}
