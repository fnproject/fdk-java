package com.fnproject.fn.runtime.exception;

import com.fnproject.fn.api.exception.FunctionLoadException;

/**
 * The function entrypoint was malformed.
 */
public class InvalidEntryPointException extends FunctionLoadException {
    public InvalidEntryPointException(String msg) {
        super(msg);
    }

    public InvalidEntryPointException(String msg, Throwable e) {
        super(msg, e);
    }
}
