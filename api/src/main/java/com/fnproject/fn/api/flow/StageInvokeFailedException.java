package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a a completion stage invocation failed within Fn - the stage may or may not have been invoked
 * and that invocation may or may not have completed.
 */
public class StageInvokeFailedException extends PlatformException {
    public StageInvokeFailedException(String reason) { super(reason); }
}
