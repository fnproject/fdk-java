package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when an external function invocation returns a failure.
 */
public class FunctionInvocationException extends RuntimeException {
    private final FunctionResponse functionResponse;

    public FunctionInvocationException(FunctionResponse functionResponse) {
        super(new String(functionResponse.getBodyAsBytes()));
        this.functionResponse = functionResponse;
    }

    public FunctionResponse getFunctionResponse() {
        return functionResponse;
    }
}
