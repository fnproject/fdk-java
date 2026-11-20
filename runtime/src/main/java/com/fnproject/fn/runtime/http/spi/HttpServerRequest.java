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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface HttpServerRequest {
    String getMethod();

    /** The unmodified request target, including any query string. */
    String getRequestTarget();

    /** Stable, repeatable header list, preserving duplicate values and their order. */
    List<HttpHeader> getHeaders();

    /**
     * The entity stream after HTTP transfer decoding, or an empty stream for no body.
     * FDK may close this stream. The provider must drain unread bytes or close the
     * connection after handling, including when FDK rejects the request.
     */
    InputStream getBody() throws IOException;
}
