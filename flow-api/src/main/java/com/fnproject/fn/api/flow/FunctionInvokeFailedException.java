package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a function call failed within the fn platform - the function may or may not have been invoked and
 * that invocation may or may not have completed.
 */
public class FunctionInvokeFailedException extends PlatformException {
    public FunctionInvokeFailedException(String reason) { super(reason); }
}
