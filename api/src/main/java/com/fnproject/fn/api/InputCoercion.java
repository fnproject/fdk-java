package com.fnproject.fn.api;

import java.util.Optional;

/**
 * Handles the coercion of an input event to a parameter
 */
public interface InputCoercion<T> {
    /**
     * An implementor of InputCoercion can throw this exception to inform the platform that the user input in the http
     * request was invalid.
     */
    class InvalidFunctionInputException extends RuntimeException {
        public InvalidFunctionInputException(String s, Throwable t) {
            super(s, t);
        }

        public InvalidFunctionInputException(String s) {
            super(s);
        }
    }

    /**
     * Handle coercion for a function parameter
     * <p>
     * Coercions may choose to act on a parameter, in which case they must return a fulfilled option) or may
     * ignore parameters (allowing other coercions to act on the parameter)
     * <p>
     * When a coercion ignores a parameter it must not consume the input stream of the event and it must return an empty
     * option.
     * <p>
     * If a coercion throws an {@link InvalidFunctionInputException}, the message will be communicated to the user in a
     * "bad request" error and no further coercions will be tried.
     * <p>
     * If a coercion throws any other RuntimeException, no further coercions will be tried and the entire function will
     * fail.
     *
     * @param currentContext the invocation context for this event - this links to the {@link com.fnproject.fn.api.RuntimeContext} and method and class
     * @param arg            the parameter index for the argument being extracted
     * @param input          the input event
     * @return the result of the coercion, if it succeeded
     */
    Optional<T> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input) throws InvalidFunctionInputException;
}
