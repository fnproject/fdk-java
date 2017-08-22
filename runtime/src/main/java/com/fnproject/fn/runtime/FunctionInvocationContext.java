package com.fnproject.fn.runtime;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.InvocationListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class FunctionInvocationContext implements InvocationContext, FunctionInvocationCallback {
    private final FunctionRuntimeContext runtimeContext;
    private List<InvocationListener> invocationListeners = new CopyOnWriteArrayList<>();

    FunctionInvocationContext(FunctionRuntimeContext ctx) {
        this.runtimeContext = ctx;
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
    public void fireOnSuccessfulInvocation() {
        for (InvocationListener listener : invocationListeners) {
            try {
                listener.onSuccess();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void fireOnFailedInvocation() {
        for (InvocationListener listener : invocationListeners) {
            try {
                listener.onFailure();
            } catch (Exception e) {
            }
        }
    }
}
