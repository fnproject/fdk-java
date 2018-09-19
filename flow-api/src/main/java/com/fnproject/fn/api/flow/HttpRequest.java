package com.fnproject.fn.api.flow;

import com.fnproject.fn.api.Headers;

/**
 * An abstract HTTP request details (without location)
 */
public interface HttpRequest {
    /**
     * Return the HTTP method used to supply this value
     *
     * @return the HTTP method
     */
    HttpMethod getMethod();

    /**
     * Return the headers on the HTTP request
     *
     * @return the headers
     */
    Headers getHeaders();

    /**
     * Returns the body of the request as a byte array
     *
     * @return the function request body
     */
    byte[] getBodyAsBytes();
}
