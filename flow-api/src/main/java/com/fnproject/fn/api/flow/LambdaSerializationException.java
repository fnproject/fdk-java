package com.fnproject.fn.api.flow;

/**
 * Exception thrown when a lambda or any referenced objects fail to be serialized.
 * The cause will typically be a {@link java.io.NotSerializableException} or other {@link java.io.IOException} detailing what could not be serialized
 */
public class LambdaSerializationException extends FlowCompletionException {
    public LambdaSerializationException(String message) {
        super(message);
    }

    public LambdaSerializationException(String message, Exception e) {
        super(message, e);
    }
}
