package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a stage failed after an internal error in the flow server, the stage may or may not have been
 * invoked and that invocation may or may not have completed.
 */
public class StageLostException extends PlatformException {
    public StageLostException(String reason) { super(reason); }
}
