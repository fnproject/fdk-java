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

package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.HttpMethod;
import com.fnproject.fn.api.flow.HttpResponse;

import java.io.IOException;
import java.io.Serializable;

/**
 * Internal Helper for  calling JSON functions via the API
 * <p>
 * Created on 21/09/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class JsonInvoke {

    private static ObjectMapper om;

    private static synchronized ObjectMapper getObjectMapper() {
        if (om == null) {
            om = new ObjectMapper();
        }
        return om;
    }

    /**
     * Invoke a JSON function on a given flow by mapping the input to JSON and composing a result stage that maps the argument back into the requested type.
     *
     * @param flow         the flow to invoke the subsequent call onto
     * @param functionId   the function ID to invoke
     * @param method       the  HTTP method to use
     * @param headers      additional headers to pass - the content
     * @param input        the input object
     * @param responseType the response type to map the function result to
     * @param <T>          the return type
     * @param <U>          the input type
     * @return a future that returns the object value of JSON function or throws an error if that function fails.
     */
    public static <T extends Serializable, U> FlowFuture<T> invokeFunction(Flow flow, String functionId, HttpMethod method, Headers headers, U input, Class<T> responseType) {

        return invokeFunction(flow, functionId, method, headers, input)
                .thenApply((httpResponse) -> {
                    try {
                        return getObjectMapper().readValue(httpResponse.getBodyAsBytes(), responseType);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to extract function response as JSON", e);
                    }
                });

    }

    /**
     * Invoke a void JSON function on a given flow by mapping the input to JSON, returns a future that completes with the HttpResponse of the function
     * if the function returns a successful http response, and completes with an error if the function invocation fails.
     *
     * @param flow       the flow to invoke the subsequent call onto
     * @param functionId the function ID to invoke
     * @param method     the  HTTP method to use
     * @param headers    additional headers to pass - the content
     * @param input      the input object
     * @param <U>        the input type
     * @return a future that returns the object value of JSON function or throws an error if that function fails.
     */
    public static <U> FlowFuture<HttpResponse> invokeFunction(Flow flow, String functionId, HttpMethod method, Headers headers, U input) {

        try {
            String inputString = getObjectMapper().writeValueAsString(input);
            Headers newHeaders;
            if (!headers.get("Content-type").isPresent()) {
                newHeaders = headers.addHeader("Content-type", "application/json");
            } else {
                newHeaders = headers;
            }
            return flow.invokeFunction(functionId, method, newHeaders, inputString.getBytes());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to coerce function input to JSON", e);
        }
    }

}
