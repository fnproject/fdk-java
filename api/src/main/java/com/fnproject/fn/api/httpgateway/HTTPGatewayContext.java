/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.api.httpgateway;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.QueryParameters;

/**
 * A context for accessing and setting HTTP Gateway atributes such aas headers and query parameters from a function call
 * <p>
 * Created on 19/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface HTTPGatewayContext {

    /**
     * Returns the underlying invocation context behind this HTTP context
     *
     * @return an invocation context related to this function
     */
    InvocationContext getInvocationContext();


    /**
     * Returns the HTTP headers for the request associated with this function call
     * If no headers were set this will return an empty headers object
     *
     * @return the incoming HTTP headers sent in the gateway request
     */
    Headers getHeaders();


    /**
     * Returns the fully qualified request URI that the function was called with, including query parameters
     *
     * @return the request URI of the function
     */
    String getRequestURL();


    /**
     * Returns the incoming request method for the HTTP
     *
     * @return the HTTP method set on this call
     */
    String getMethod();

    /**
     * Returns the query parameters of the request
     *
     * @return a query parameters object
     */
    QueryParameters getQueryParameters();


    /**
     * Adds a response header to the outbound event
     *
     * @param key   header key
     * @param value header value
     */
    void addResponseHeader(String key, String value);

    /**
     * Sets a response header to the outbound event, overriding a previous value.
     * <p>
     * Headers set in this way override any headers returned by the function or any middleware on the function
     * <p>
     * Setting the "Content-Type" response header also sets this on the underlying Invocation context
     *
     * @param key header key
     * @param v1  first value to set
     * @param vs  other values to set header to
     */
    void setResponseHeader(String key, String v1, String... vs);

    /**
     * Sets the HTTP status code of the response
     *
     * @param code an HTTP status code
     * @throws IllegalArgumentException if the code is &lt; 100 or &gt;l=600
     */
    void setStatusCode(int code);
}
