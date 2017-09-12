package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a lambda or any referenced objects fail to be serialized.
 */
public class LambdaSerializationException extends FlowCompletionException {
    public LambdaSerializationException(String message) {
        super(message);
    }
}
