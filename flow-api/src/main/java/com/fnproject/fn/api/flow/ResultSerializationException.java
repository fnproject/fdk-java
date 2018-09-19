package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a result returned by a completion stage fails to be serialized.
 */
public class ResultSerializationException extends FlowCompletionException {
    public ResultSerializationException(String message, Throwable e) {
        super(message, e);
    }
}
