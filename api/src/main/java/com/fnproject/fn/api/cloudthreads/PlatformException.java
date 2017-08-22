package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when the completion facility fails to operate on a completion graph
 */
public class PlatformException extends CloudCompletionException {
    public PlatformException(Throwable t) {
        super(t);
    }
    public PlatformException(String message) {
        super(message);
    }
    public PlatformException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * These are manufactured exceptions that arise outside the current runtime; therefore,
     * the notion of an embedded stack trace is meaningless.
     *
     * @return this
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
