package com.fnproject.fn.api.exception;

/**
 * The function class's configuration methods could not be invoked.
 */
public class FunctionConfigurationException extends FunctionLoadException {

    public FunctionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionConfigurationException(String message) {
        super(message);
    }
}
