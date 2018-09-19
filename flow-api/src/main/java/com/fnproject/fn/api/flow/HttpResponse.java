package com.fnproject.fn.api.flow;

import com.fnproject.fn.api.Headers;

/**
 * A FunctionResponse represents the HTTP response from an external function invocation
 * (e.g. execute by {@code rt.invokeFunction(id, data)}
 */
public interface HttpResponse {
    /**
     * Return the HTTP status code of the function response
     *
     * @return the HTTP status code
     */
    int getStatusCode();

    /**
     * Return the headers on the HTTP function response
     *
     * @return the headers
     */
    Headers getHeaders();

    /**
     * Returns the body of the function result as a byte array
     *
     * @return the function response body
     */
    byte[] getBodyAsBytes();
}
