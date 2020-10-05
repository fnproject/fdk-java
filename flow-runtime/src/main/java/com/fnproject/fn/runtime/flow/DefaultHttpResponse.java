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

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.HttpResponse;

import java.util.Objects;

/**
 * Created on 27/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class DefaultHttpResponse implements HttpResponse {


    private final int statusCode;
    private final Headers headers;
    private final byte[] body;

    public DefaultHttpResponse(int statusCode, Headers headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = Objects.requireNonNull(headers);
        this.body = Objects.requireNonNull(body);
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public byte[] getBodyAsBytes() {
        return body;
    }
}
