package com.fnproject.fn.runtime.exception;

import com.fnproject.fn.api.exception.FunctionLoadException;

/**
 * the function definition passed was invalid (e.g. class or method did not exist in jar, or method did not match required signature)
 */
public class InvalidFunctionDefinitionException extends FunctionLoadException {

    public InvalidFunctionDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFunctionDefinitionException(String message) {
        super(message);
    }
}
