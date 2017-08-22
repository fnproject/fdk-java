package com.fnproject.fn.api;

import java.util.EventListener;

/**
 * Listener that will be notified in the event that a function invocation executes successfully or fails.
 * error. Instances of InvocationListener should be registered with {@link InvocationContext}.
 */
public interface InvocationListener extends EventListener {

    /**
     * Notifies this InvocationListener that a function invocation has executed successfully.
     */
    void onSuccess();

    /**
     * Notifies this InvocationListener that a function invocation has failed during its execution.
     */
    void onFailure();

}
