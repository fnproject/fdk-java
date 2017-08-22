package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when a result returned by a completion stage fails to be serialized.
 */
public class ResultSerializationException extends CloudCompletionException {
    public ResultSerializationException(String message, Throwable e) {
        super(message, e);
    }
}
