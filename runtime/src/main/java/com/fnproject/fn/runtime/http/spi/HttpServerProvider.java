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

import com.fnproject.fn.runtime.EventCodec;

import java.util.Map;

public interface HttpServerProvider {

    /**
     * Creates the codec before function initialization.
     *
     * @param environment read-only snapshot of the runtime environment
     * @param fdkVersion value for the {@code Fn-Fdk-Version} response header
     * @param runtimeVersion value for the {@code Fn-Fdk-Runtime} response header
     * @return a non-null codec; the runtime will call {@link EventCodec#runCodec}
     * @throws Exception if the server cannot be created
     */
    EventCodec create(Map<String, String> environment, String fdkVersion,
                      String runtimeVersion) throws Exception;
}
