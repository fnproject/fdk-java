package com.fnproject.fn.runtime.exception;

/**
 * The FDK was not able to start up
 */
public final class FunctionInitializationException extends RuntimeException {


    /**
     * create a function invocation exception
     *
     * @param message private message for this exception -
     * @param target  the underlying user exception that triggered this failure
     */
    public FunctionInitializationException(String message, Throwable target) {
        super(message, target);
    }


    public FunctionInitializationException(String message) {
        super(message);
    }
}
