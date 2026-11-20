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

import com.fnproject.fn.runtime.exception.FunctionIOException;
import com.fnproject.fn.runtime.http.spi.HttpServer;

import java.io.IOException;

final class HttpServerCodec implements EventCodec {
    private final HttpServer server;
    private final String fdkVersion;
    private final String runtimeVersion;

    HttpServerCodec(HttpServer server, String fdkVersion, String runtimeVersion) {
        this.server = server;
        this.fdkVersion = fdkVersion;
        this.runtimeVersion = runtimeVersion;
    }

    @Override
    public void runCodec(Handler handler) {
        try (HttpServer transport = server) {
            transport.run(new FnHttpHandler(handler, fdkVersion, runtimeVersion));
        } catch (IOException e) {
            throw new FunctionIOException("HTTP server failed", e);
        }
    }
}
