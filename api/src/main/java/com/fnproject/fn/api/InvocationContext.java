package com.fnproject.fn.api;


/**
 * Context wrapper around a single invocation.
 * <p>
 * Any attributes attached to this interface are scoped solely to a single function invocation;
 * multiple invocations of a hot function will receive new instances of this interface.
 */
public interface InvocationContext {

    RuntimeContext getRuntimeContext();

    /**
     * Adds an {@link InvocationListener} that will be fired when an invocation of this function
     * completes either successfully or exceptionally.
     *
     * @param listener a listener to fire when function completes execution
     */
    void addListener(InvocationListener listener);

}
