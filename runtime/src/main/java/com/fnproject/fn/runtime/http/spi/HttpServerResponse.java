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

public interface HttpServerResponse {
    void setStatus(int statusCode, String reasonPhrase);

    /** Append a value without combining repeated headers. */
    void addHeader(String name, String value);

    /** Replace the first matching header, case-insensitively, or append if absent. */
    void setHeader(String name, String value);

    /**
     * Replace the buffered entity. Content type is entity metadata and must not
     * overwrite an explicit Content-Type header. An empty array is an empty body.
     * FDK will not modify the supplied bytes after this call.
     */
    void setBody(byte[] body, String contentType);
}
