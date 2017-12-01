package com.fnproject.fn.api.exception;

/**
 * This used to wrap any exception thrown by an {@link com.fnproject.fn.api.InputCoercion}. It is
 * also thrown if no {@link com.fnproject.fn.api.InputCoercion} is applicable to a parameter of the user function.
 *
 * This indicates that the input was not appropriate to this function.
 */
public class FunctionInputHandlingException extends RuntimeException {
    public FunctionInputHandlingException(String s, Throwable t) {
        super(s,t);
    }

    public FunctionInputHandlingException(String s) {
        super(s);
    }
}
