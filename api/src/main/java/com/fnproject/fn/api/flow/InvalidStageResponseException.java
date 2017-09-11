package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a completion stage responds with an incompatible datum type for its corresponding completion
 * graph stage.
 */
public class InvalidStageResponseException extends PlatformException {
    public InvalidStageResponseException(String reason) { super(reason); }
}
