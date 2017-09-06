package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;

/**
 * A simple abstraction for a parsed HTTP response returned by a function
 */
public interface FnResult {
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
     * A map of the headers returned by the function
     * <p>
     * These are squashed so duplicated headers will be ignored (takes the first header).
     *
     * @return a map of headers
     */
    Headers getHeaders();

    /**
     * Returns the HTTP status code of the function response
     *
     * @return the HTTP status code returned by the function
     */
    int getStatus();
}
