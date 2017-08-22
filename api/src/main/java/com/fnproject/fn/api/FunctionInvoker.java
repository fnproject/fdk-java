package com.fnproject.fn.api;

import java.util.Optional;

/**
 * Handles the invocation of a given function call
 */
public interface FunctionInvoker {
    /**
     * Optionally handles an invocation chain for this function
     * <p>
     * If the invoker returns an empty option then no action has been taken and another invoker may attempt to tryInvoke
     * <p>
     * In particular this means that implementations must not read the input event body until they are committed to handling this event.
     *
     * A RuntimeException thrown by the implementation will cause the entire function invocation to fail.
     *
     * @param ctx the context for a single invocation
     * @param evt the incoming event
     * @return a optional output event if the invoker handled the event.
     */
    Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt);
}
