package com.fnproject.fn.runtime.exception;

public class FunctionClassInstantiationException extends FunctionLoadException {
    public FunctionClassInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionClassInstantiationException(String s) {
        super(s);
    }
}
