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
