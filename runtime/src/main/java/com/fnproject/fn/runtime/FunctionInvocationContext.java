package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.InvocationListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Function invocation context implementation,
 * Delegates invocation callbacks to configured listeners
 */
public class FunctionInvocationContext implements InvocationContext, FunctionInvocationCallback {
    private final FunctionRuntimeContext runtimeContext;
    private final List<InvocationListener> invocationListeners = new CopyOnWriteArrayList<>();

    private final InputEvent event;
    private final Map<String, List<String>> additionalResponseHeaders = new ConcurrentHashMap<>();

    FunctionInvocationContext(FunctionRuntimeContext ctx, InputEvent event) {
        this.runtimeContext = ctx;
        this.event = event;
    }

    @Override
    public FunctionRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public void addListener(InvocationListener listener) {
        invocationListeners.add(listener);
    }

    @Override
    public Headers getRequestHeaders() {
        return event.getHeaders();
    }

    @Override
    public void addResponseHeader(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        additionalResponseHeaders.merge(key, Collections.singletonList(value), (a, b) -> {
            List<String> l = new ArrayList<>(a);
            l.addAll(b);
            return l;
        });
    }

    /**
     * returns the internal map of added response headers
     *
     * @return mutable map of internal response headers
     */
    Map<String, List<String>> getAdditionalResponseHeaders() {
        return additionalResponseHeaders;
    }

    @Override
    public void setResponseHeader(String key, String value, String... vs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(vs, "vs");
        Arrays.stream(vs).forEach(v->Objects.requireNonNull(v,"null value in list "));

        String cKey = Headers.canonicalKey(key);
        if (value == null) {
            additionalResponseHeaders.remove(cKey);
            return;
        }
        additionalResponseHeaders.put(cKey, Collections.singletonList(value));
    }

    @Override
    public void fireOnSuccessfulInvocation() {
        for (InvocationListener listener : invocationListeners) {
            try {
                listener.onSuccess();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void fireOnFailedInvocation() {
        for (InvocationListener listener : invocationListeners) {
            try {
                listener.onFailure();
            } catch (Exception ignored) {
            }
        }
    }
}
