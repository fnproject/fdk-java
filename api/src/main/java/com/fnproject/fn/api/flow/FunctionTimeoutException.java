package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a function execution exceeds its configured timeout.
 *
 * When this exception is raised the fn server has terminated the container hosting the function.
 */
public class FunctionTimeoutException extends PlatformException {
    public FunctionTimeoutException(String reason) { super(reason); }
}
