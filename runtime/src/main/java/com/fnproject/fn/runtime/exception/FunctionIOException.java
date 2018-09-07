package com.fnproject.fn.runtime.exception;

/**
 * The FDK experienced a terminal issue communicating with the platform
 */
public final class FunctionIOException extends RuntimeException {


    /**
     * create a function invocation exception
     *
     * @param message private message for this exception -
     * @param target  the underlying user exception that triggered this failure
     */
    public FunctionIOException(String message, Throwable target) {
        super(message, target);
    }


}
