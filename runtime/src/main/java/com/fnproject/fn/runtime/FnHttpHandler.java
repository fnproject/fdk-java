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

import com.fasterxml.jackson.core.io.CharTypes;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.http.spi.HttpHandler;
import com.fnproject.fn.runtime.http.spi.HttpHeader;
import com.fnproject.fn.runtime.http.spi.HttpServerRequest;
import com.fnproject.fn.runtime.http.spi.HttpServerResponse;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class FnHttpHandler implements HttpHandler {
    private static final Set<String> stripInputHeaders;
    private static final Set<String> stripOutputHeaders;
    private final EventCodec.Handler handler;
    private final String fdkVersion;
    private final String runtimeVersion;

    FnHttpHandler(EventCodec.Handler handler, String fdkVersion, String runtimeVersion) {
        this.handler = Objects.requireNonNull(handler, "handler");
        this.fdkVersion = Objects.requireNonNull(fdkVersion, "fdkVersion");
        this.runtimeVersion = Objects.requireNonNull(runtimeVersion, "runtimeVersion");
    }

    static {
        Set<String> hin = new HashSet<>();
        hin.add("Host");
        hin.add("Accept-Encoding");
        hin.add("Transfer-Encoding");
        hin.add("User-Agent");
        hin.add("Connection");
        hin.add("TE");

        stripInputHeaders = Collections.unmodifiableSet(hin);

        Set<String> hout = new HashSet<>();
        hout.add("Content-Length");
        hout.add("Transfer-Encoding");
        hout.add("Connection");
        hout.add("Fn-Fdk-Version");

        stripOutputHeaders = Collections.unmodifiableSet(hout);
    }


    @Override
    public synchronized void handle(HttpServerRequest request, HttpServerResponse response) {
        // Match the legacy HttpCore router: exact /call path, any HTTP method.
        String path = request.getRequestTarget();
        int end = path.indexOf('?');
        if (end < 0) {
            end = path.indexOf('#');
        }
        if (end >= 0) {
            path = path.substring(0, end);
        }
        if (!"/call".equals(path)) {
            response.setStatus(501, "Not Implemented");
            return;
        }

        InputEvent event;
        try {
            event = readEvent(request);
        } catch (FunctionInputHandlingException e) {
            error(response, "Invalid input for function", e.getMessage());
            return;
        }

        OutputEvent output;
        try {
            output = handler.handle(event);
        } catch (Exception e) {
            error(response, "Unhandled internal error in FDK", e.getMessage());
            return;
        }

        try {
            writeEvent(output, response);
        } catch (Exception e) {
            error(response, "Unhandled internal error while writing FDK response", e.getMessage());
        }
    }

    private void error(HttpServerResponse response, String message, String detail) {
        response.setStatus(500, "Internal Server Error");
        response.setBody(jsonError(message, detail).getBytes(StandardCharsets.UTF_8),
            ContentType.APPLICATION_JSON.toString());
    }

    private String jsonError(String message, String detail) {
        if (message == null) {
            message = "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{ \"message\":\"");
        CharTypes.appendQuoted(sb, message);
        sb.append("\"");

        if (detail != null) {
            sb.append(", \"detail\":\"");
            CharTypes.appendQuoted(sb, detail);
            sb.append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String getRequiredHeader(HttpServerRequest request, String headerName) {
        for (HttpHeader header : request.getHeaders()) {
            if (headerName.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        throw new FunctionInputHandlingException("Required FDK header variable " + headerName
            + " is not set, check you are using the latest fn and FDK versions");
    }

    private InputEvent readEvent(HttpServerRequest request) {

        InputStream bodyStream;
        try {
            bodyStream = request.getBody();
        } catch (IOException e) {
            throw new FunctionInputHandlingException("error handling input", e);
        }

        String deadline = getRequiredHeader(request, "Fn-Deadline");
        String callID = getRequiredHeader(request, "Fn-Call-Id");

        if (callID == null) {
            callID = "";
        }
        Instant deadlineDate = Instant.now().plus(1, ChronoUnit.HOURS);
        if (deadline != null) {
            try {
                deadlineDate = Instant.parse(deadline);
            } catch (DateTimeParseException e) {
                throw new FunctionInputHandlingException("Invalid deadline date format", e);
            }
        }
        Headers headersIn = Headers.emptyHeaders();


        for (HttpHeader h : request.getHeaders()) {
            if (stripInputHeaders.contains(Headers.canonicalKey(h.getName()))) {
                continue;
            }
            headersIn = headersIn.addHeader(h.getName(), h.getValue());
        }

        return new ReadOnceInputEvent(bodyStream, headersIn, callID, deadlineDate);

    }

    private void writeEvent(OutputEvent evt, HttpServerResponse response) {

        evt.getHeaders().asMap()
            .entrySet()
            .stream()
            .filter(e -> !stripOutputHeaders.contains(e.getKey()))
            .flatMap(e -> e.getValue().stream().map((v) -> new HttpHeader(e.getKey(), v)))
            .forEachOrdered(header -> response.addHeader(header.getName(), header.getValue()));

        ContentType contentType = evt.getContentType().map(c -> {
            try {
                return ContentType.parse(c);
            } catch (ParseException e) {
                return ContentType.DEFAULT_BINARY;
            }
        }).orElse(ContentType.DEFAULT_BINARY);

        response.setHeader("Content-Type", contentType.toString());
        response.setHeader("Fn-Fdk-Version", fdkVersion);
        response.setHeader("Fn-Fdk-Runtime", runtimeVersion);

        response.setStatus(evt.getStatus().getCode(), evt.getStatus().name());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // TODO remove output buffering here - possibly change OutputEvent contract to support providing an InputStream?
        try {
            evt.writeToOutput(bos);
        } catch (IOException e) {
            throw new FunctionOutputHandlingException("Error writing output", e);
        }
        byte[] data = bos.toByteArray();
        response.setBody(data, contentType.toString());

    }


}
