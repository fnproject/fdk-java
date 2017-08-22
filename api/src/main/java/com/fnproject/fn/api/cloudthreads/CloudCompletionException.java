package com.fnproject.fn.api.cloudthreads;


/**
 * Exception thrown when a blocking operation on a cloud thread fails - this corresponds to a
 * {@link java.util.concurrent.CompletionException} in {@link java.util.concurrent.CompletableFuture} calls
 */
public class CloudCompletionException extends RuntimeException {

    /**
     * If an exception was raised from within a continuation, this will be the wrapped cause.
     * @param t  The user exception
     */
    public CloudCompletionException(Throwable t) {
        super(t);
    }

    public CloudCompletionException(String message) {
        super(message);
    }

    public CloudCompletionException(String message, Throwable t) {
        super(message, t);
    }

}
