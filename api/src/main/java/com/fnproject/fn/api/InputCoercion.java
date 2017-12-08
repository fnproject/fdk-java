package com.fnproject.fn.api;

import java.util.Optional;

/**
 * Handles the coercion of an input event to a parameter
 */
public interface InputCoercion<T> {
    /**
     * Handle coercion for a function parameter
     * <p>
     * Coercions may choose to act on a parameter, in which case they should return a fulfilled option) or may
     * ignore parameters (allowing other coercions to act on the parameter)
     * <p>
     * When a coercion ignores a parameter it must not consume the input stream of the event.
     * <p>
     * If a coercion throws a RuntimeException, no further coercions will be tried and the function invocation will fail.
     *
     * @param currentContext the invocation context for this event - this links to the {@link RuntimeContext} and method and class
     * @param arg            the parameter index for the argument being extracted
     * @param input          the input event
     * @param methodWrapper  the method which the parameter is for
     * @return               the result of the coercion, if it succeeded
     */
    Optional<T> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper methodWrapper);

}
