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

import java.util.Map;

public interface HttpServerProvider {
    /**
     * Creates the server before function initialization. It must not start
     * handling requests until {@link HttpServer#run(HttpHandler)} is called.
     *
     * @param environment read-only snapshot containing FN_LISTENER and other settings
     * @return a non-null server; FDK closes it when its run method exits
     * @throws Exception if the server cannot be created; release partial resources
     */
    HttpServer create(Map<String, String> environment) throws Exception;
}
