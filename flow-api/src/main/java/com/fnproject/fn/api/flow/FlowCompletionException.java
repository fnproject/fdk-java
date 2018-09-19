package com.fnproject.fn.api.flow;


/**
 * Exception thrown when a blocking operation on a flow fails - this corresponds to a
 * {@link java.util.concurrent.CompletionException} in {@link java.util.concurrent.CompletableFuture} calls
 */
public class FlowCompletionException extends RuntimeException {

    /**
     * If an exception was raised from within a stage, this will be the wrapped cause.
     * @param t  The user exception
     */
    public FlowCompletionException(Throwable t) {
        super(t);
    }

    public FlowCompletionException(String message) {
        super(message);
    }

    public FlowCompletionException(String message, Throwable t) {
        super(message, t);
    }

}
