package com.fnproject.fn.runtime.exception;

import com.fnproject.fn.api.exception.FunctionLoadException;

public class FunctionClassInstantiationException extends FunctionLoadException {
    public FunctionClassInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionClassInstantiationException(String s) {
        super(s);
    }
}
