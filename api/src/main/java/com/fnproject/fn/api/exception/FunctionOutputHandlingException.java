package com.fnproject.fn.api.exception;

/**
 * This used to wrap any exception thrown by an {@link com.fnproject.fn.api.OutputCoercion}. It is
 * also thrown if no {@link com.fnproject.fn.api.OutputCoercion} is applicable to the object returned by the function.
 */
public class FunctionOutputHandlingException extends RuntimeException {
    public FunctionOutputHandlingException(String s, Exception e) {
        super(s,e);
    }

    public FunctionOutputHandlingException(String s) {
        super(s);

    }
}
