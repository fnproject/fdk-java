package com.fnproject.fn.runtime.exception;

/**
 * An exception thrown when the function format is invalid or not supported by the Java FDK, therefore the function
 * code cannot be invoked.
 */
public class FunctionFormatException extends FunctionLoadException {
    
    public FunctionFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionFormatException(String s) {
        super(s);
    }
}
