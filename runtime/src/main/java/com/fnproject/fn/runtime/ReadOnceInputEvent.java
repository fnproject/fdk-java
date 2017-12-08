package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;

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
    private AtomicBoolean consumed = new AtomicBoolean(false);
    private QueryParameters queryParameters;
    private final Headers headers;


    public ReadOnceInputEvent(String appName, String route, String requestUrl, String method, InputStream body, Headers headers, QueryParameters parameters) {
        this.appName = Objects.requireNonNull(appName);
        this.route = Objects.requireNonNull(route);
        this.requestUrl = Objects.requireNonNull(requestUrl);
        this.method = Objects.requireNonNull(method).toUpperCase();
        this.body = new BufferedInputStream(Objects.requireNonNull(body));
        this.headers = Objects.requireNonNull(headers);
        this.queryParameters = Objects.requireNonNull(parameters);
        body.mark(Integer.MAX_VALUE);
    }


    /**
     * Consume the input stream of this event  -
     * This may be done exactly once per event
     *
     * @param dest a consumer for the body
     * @throws IllegalStateException if the input has been consumed
     */
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

    /**
     * @return The fn application name associated with this call
     */
    @Override
    public String getAppName() {
        return appName;
    }

    /**
     * @return The route associated with this call (starting with a slash)
     */
    @Override
    public String getRoute() {
        return route;
    }

    /**
     * @return The full request URL into the app
     */
    @Override
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * @return The HTTP method (capitalised) of this request
     */
    @Override
    public String getMethod() {
        return method;
    }

    /**
     * The HTTP headers on the request
     *
     * @return an immutable map of headers
     */
    @Override
    public Headers getHeaders() {
        return headers;
    }

    /**
     * The query parameters of the function invocation
     *
     * @return an immutable map of query parameters parsed from the request URL
     */
    @Override
    public QueryParameters getQueryParameters() {
        return queryParameters;
    }

    @Override
    public void close() throws IOException {
        body.close();
    }
}
