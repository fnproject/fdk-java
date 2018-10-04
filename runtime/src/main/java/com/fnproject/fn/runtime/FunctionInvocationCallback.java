package com.fnproject.fn.runtime;

public interface FunctionInvocationCallback {
    void fireOnSuccessfulInvocation();

    void fireOnFailedInvocation();

}
