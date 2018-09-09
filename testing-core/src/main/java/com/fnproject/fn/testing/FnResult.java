package com.fnproject.fn.testing;

import com.fnproject.fn.api.OutputEvent;

/**
 * A simple abstraction over {@link OutputEvent} that buffers the response body
 */
public interface FnResult  extends OutputEvent {
    /**
     * Returns the body of the function result as a byte array
     *
     * @return the function response body
     */
    byte[] getBodyAsBytes();

    /**
     * Returns the body of the function response as a string
     *
     * @return a function response body
     */
    String getBodyAsString();


    /**
     * Determine if the status code corresponds to a successful invocation
     *
     * @return true if the status code indicates success
     */
    default boolean isSuccess() {
        return getStatus() == Status.Success;
    }
}
