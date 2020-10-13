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

package com.fnproject.fn.runtime.httpgateway;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created on 19/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FunctionHTTPGatewayContext implements HTTPGatewayContext {

    private final InvocationContext invocationContext;
    private final Headers httpRequestHeaders;
    private final String method;
    private final String requestUrl;
    private final QueryParameters queryParameters;

    public FunctionHTTPGatewayContext(InvocationContext invocationContext) {
        this.invocationContext = Objects.requireNonNull(invocationContext, "invocationContext");

        Map<String, List<String>> myHeaders = new HashMap<>();

        String requestUri = "";
        String method = "";
        for (Map.Entry<String, List<String>> e : invocationContext.getRequestHeaders().asMap().entrySet()) {
            String key = e.getKey();
            if (key.startsWith("Fn-Http-H-")) {
                String httpKey = key.substring("Fn-Http-H-".length());
                if (httpKey.length() > 0) {
                    myHeaders.put(httpKey, e.getValue());
                }
            }

            if (key.equals("Fn-Http-Request-Url")) {
                requestUri = e.getValue().get(0);
            }
            if (key.equals("Fn-Http-Method")) {
                method = e.getValue().get(0);
            }

        }
        this.queryParameters = QueryParametersParser.getParams(requestUri);
        this.requestUrl = requestUri;
        this.method = method;
        this.httpRequestHeaders = Headers.emptyHeaders().setHeaders(myHeaders);

    }

    @Override
    public InvocationContext getInvocationContext() {
        return invocationContext;
    }

    @Override
    public Headers getHeaders() {
        return httpRequestHeaders;
    }

    @Override
    public String getRequestURL() {
        return requestUrl;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public QueryParameters getQueryParameters() {
        return queryParameters;
    }

    @Override
    public void addResponseHeader(String key, String value) {
        invocationContext.addResponseHeader("Fn-Http-H-" + key, value);

    }

    @Override
    public void setResponseHeader(String key, String value, String... vs) {

        if (Headers.canonicalKey(key).equals(OutputEvent.CONTENT_TYPE_HEADER)) {
            invocationContext.setResponseContentType(value);
            invocationContext.setResponseHeader("Fn-Http-H-" + key, value);
        } else {
            invocationContext.setResponseHeader("Fn-Http-H-" + key, value, vs);

        }


    }

    @Override
    public void setStatusCode(int code) {
        if (code < 100 || code >= 600) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + code);
        }
        invocationContext.setResponseHeader("Fn-Http-Status", "" + code);
    }
}
