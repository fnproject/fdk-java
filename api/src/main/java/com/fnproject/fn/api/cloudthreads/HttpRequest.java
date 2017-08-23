package com.fnproject.fn.api.cloudthreads;

import com.fnproject.fn.api.Headers;

/**
 * An externally completable future's value will be of this type.
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
