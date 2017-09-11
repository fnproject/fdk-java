package com.fnproject.fn.api.flow;

/**
 * Exception thrown when an external function invocation returns a failure.
 *
 * This includes when the function returns a result but has a non-successful HTTP error status
 *
 */
public class FunctionInvocationException extends RuntimeException {
    private final HttpResponse functionResponse;

    public FunctionInvocationException(HttpResponse functionResponse) {
        super(new String(functionResponse.getBodyAsBytes()));
        this.functionResponse = functionResponse;
    }

    /**
     * The HTTP details returned from the function invocation
     * @return an http response from the an external function
     */
    public HttpResponse getFunctionResponse() {
        return functionResponse;
    }
}
