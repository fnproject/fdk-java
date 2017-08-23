package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when an external function invocation returns a failure.
 */
public class FunctionInvocationException extends RuntimeException {
    private final HttpResponse functionResponse;

    public FunctionInvocationException(HttpResponse functionResponse) {
        super(new String(functionResponse.getBodyAsBytes()));
        this.functionResponse = functionResponse;
    }

    public HttpResponse getFunctionResponse() {
        return functionResponse;
    }
}
