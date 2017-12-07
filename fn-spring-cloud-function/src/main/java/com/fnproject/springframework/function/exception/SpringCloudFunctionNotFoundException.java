package com.fnproject.springframework.function.exception;

import com.fnproject.fn.api.exception.FunctionLoadException;

/**
 * Spring Cloud Function integration attempts to find the right Bean to use
 * as the function entrypoint. This exception is thrown when that fails.
 */
public class SpringCloudFunctionNotFoundException extends FunctionLoadException {

    public SpringCloudFunctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpringCloudFunctionNotFoundException(String msg) {
        super(msg);
    }
}
