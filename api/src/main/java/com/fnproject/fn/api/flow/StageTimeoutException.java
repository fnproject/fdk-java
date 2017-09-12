package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a completion stage function execution exceeds it configured timeout -
 * the stage may or may not have completed normally.
 *
 * When this exception is raised the fn server has terminated the container hosting the function.
 */
public class StageTimeoutException extends PlatformException {
    public StageTimeoutException(String reason) { super(reason); }
}
