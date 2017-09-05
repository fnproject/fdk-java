package com.fnproject.fn.runtime.exception;

import org.springframework.beans.BeansException; /**
 * The function entrypoint was malformed .
 */
public class InvalidEntryPointException extends FunctionLoadException {
    public InvalidEntryPointException(String msg) {
        super(msg);
    }

    public InvalidEntryPointException(String msg, Throwable e) {
        super(msg, e);
    }
}
