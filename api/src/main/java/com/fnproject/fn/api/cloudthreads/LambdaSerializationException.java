package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when a lambda or any referenced objects fail to be serialized.
 */
public class LambdaSerializationException extends CloudCompletionException {
    public LambdaSerializationException(String message) {
        super(message);
    }
}
