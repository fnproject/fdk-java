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

package com.fnproject.fn.runtime.http.spi;

import java.io.Closeable;
import java.io.IOException;

public interface HttpServer extends Closeable {
    /**
     * Serve requests until shutdown or a fatal error. Call the handler on workers
     * that may block, never on an event loop. Keep each exchange alive until its
     * callback returns, then send the response once using valid HTTP framing.
     * The provider owns admission limits, connections, and listener publication.
     * FDK serializes callbacks, including function execution and output conversion.
     * Stop accepting work and wait for workers to finish before returning.
     *
     * @param handler the shared FDK HTTP handler, including routing
     * @throws IOException on a fatal transport failure
     */
    void run(HttpHandler handler) throws IOException;

    /** Release owned sockets and workers, even after a failed run. Must be idempotent. */
    @Override
    void close() throws IOException;
}
