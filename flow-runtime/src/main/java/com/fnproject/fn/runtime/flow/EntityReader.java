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

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Both an HTTP response and an individual part of a multipart MIME stream are constituted of
 * a set of headers together with the body stream. This interface abstracts the access to those parts.
 */
interface EntityReader {
    String getHeaderElement(String h, String e);

    Optional<String> getHeaderValue(String header);

    InputStream getContentStream();

    Map<String, String> getHeaders();
}
