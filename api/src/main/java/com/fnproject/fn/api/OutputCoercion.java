package com.fnproject.fn.api;

import java.util.Optional;


/**
 * Handles the coercion of a function's return value to an {@link OutputEvent}
 */
public interface OutputCoercion {

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
     * @param method         the method which was invoked
     * @param value          The object which the method returned
     * @return               the result of the coercion, if it succeeded
     */
    Optional<OutputEvent> wrapFunctionResult(InvocationContext currentContext, MethodWrapper method, Object value);
}
