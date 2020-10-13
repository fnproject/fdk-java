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

/**
 * Exception thrown when an external function invocation returns a failure.
 *
 * This includes when the function returns a result but has a non-successful HTTP error status
 *
 */
public class FunctionInvocationException extends RuntimeException {
    private final HttpResponse functionResponse;

    public FunctionInvocationException(HttpResponse functionResponse) {
        super(new String(functionResponse.getBodyAsBytes()));
        this.functionResponse = functionResponse;
    }

    /**
     * The HTTP details returned from the function invocation
     * @return an http response from the an external function
     */
    public HttpResponse getFunctionResponse() {
        return functionResponse;
    }
}
