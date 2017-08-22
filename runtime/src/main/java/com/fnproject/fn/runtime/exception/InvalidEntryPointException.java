package com.fnproject.fn.runtime.exception;

/**
 * The function entrypoint was malformed .
 */
public class InvalidEntryPointException extends FunctionLoadException {
    public InvalidEntryPointException(String msg) {
        super(msg);
    }


}
