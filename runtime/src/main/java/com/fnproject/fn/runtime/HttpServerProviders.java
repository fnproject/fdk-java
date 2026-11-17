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

import com.fnproject.fn.runtime.exception.FunctionInitializationException;
import com.fnproject.fn.runtime.http.spi.HttpServerProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

final class HttpServerProviders {
    private HttpServerProviders() {
    }

    static EventCodec create(Map<String, String> environment, String fdkVersion, String runtimeVersion) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = HttpServerProvider.class.getClassLoader();
        }

        HttpServerProvider provider = null;
        try {
            Iterator<HttpServerProvider> providers = ServiceLoader.load(HttpServerProvider.class, loader).iterator();
            if (providers.hasNext()) {
                provider = providers.next();
                if (providers.hasNext()) {
                    throw new FunctionInitializationException("Multiple HttpServerProvider implementations registered; "
                        + "register exactly one HTTP server provider");
                }
            }
        } catch (ServiceConfigurationError | LinkageError e) {
            throw new FunctionInitializationException("Unable to load HttpServerProvider", e);
        }

        if (provider == null) {
            return new HTTPStreamCodec(environment, fdkVersion, runtimeVersion);
        }

        try {
            EventCodec codec = provider.create(Collections.unmodifiableMap(new HashMap<>(environment)),
                fdkVersion, runtimeVersion);
            if (codec == null) {
                throw new FunctionInitializationException("HttpServerProvider " + provider.getClass().getName()
                    + " returned a null codec");
            }
            return codec;
        } catch (Exception | ServiceConfigurationError | LinkageError e) {
            throw new FunctionInitializationException("Unable to create HTTP server using "
                + provider.getClass().getName(), e);
        }
    }
}
