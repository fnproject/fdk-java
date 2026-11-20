/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
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

package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.http.spi.HttpHeader;
import com.fnproject.fn.runtime.http.spi.HttpServerRequest;
import com.fnproject.fn.runtime.http.spi.HttpServerResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class HttpTestExchange implements HttpServerRequest, HttpServerResponse {
    String target = "/call";
    List<HttpHeader> requestHeaders = new ArrayList<>(Arrays.asList(
        new HttpHeader("Fn-Call-Id", "call-id"),
        new HttpHeader("Fn-Deadline", "2002-10-02T10:00:00.992Z")));
    InputStream input = new ByteArrayInputStream(new byte[0]);
    int statusCode = 200;
    String reasonPhrase;
    List<HttpHeader> responseHeaders = new ArrayList<>();
    byte[] body;
    String contentType;

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public String getRequestTarget() {
        return target;
    }

    @Override
    public List<HttpHeader> getHeaders() {
        return requestHeaders;
    }

    @Override
    public InputStream getBody() {
        return input;
    }

    @Override
    public void setStatus(int code, String reason) {
        statusCode = code;
        reasonPhrase = reason;
    }

    @Override
    public void addHeader(String name, String value) {
        responseHeaders.add(new HttpHeader(name, value));
    }

    @Override
    public void setHeader(String name, String value) {
        for (int i = 0; i < responseHeaders.size(); i++) {
            if (name.equalsIgnoreCase(responseHeaders.get(i).getName())) {
                responseHeaders.set(i, new HttpHeader(name, value));
                return;
            }
        }
        addHeader(name, value);
    }

    @Override
    public void setBody(byte[] bytes, String type) {
        body = bytes;
        contentType = type;
    }

    String header(String name) {
        return responseHeaders.stream().filter(h -> name.equalsIgnoreCase(h.getName()))
            .map(HttpHeader::getValue).findFirst().orElse(null);
    }
}
