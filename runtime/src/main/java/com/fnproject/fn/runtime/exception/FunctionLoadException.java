package com.fnproject.fn.runtime.exception;




/**
 * an exception relating to loading the users code into runtime
 * see children for specific cases
 */
public abstract class FunctionLoadException extends RuntimeException {

    public FunctionLoadException(String message, Throwable cause) {
        super(message, cause);
    }


    public FunctionLoadException(String msg) {
        super(msg);
    }
}
