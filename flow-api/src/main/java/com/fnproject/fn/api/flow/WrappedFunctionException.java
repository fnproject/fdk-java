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

package com.fnproject.fn.api.flow;

import java.io.Serializable;

/**
 * Wrapped exception where the root cause of a function failure was not serializable.
 *
 * This exposes the type of the original exception via {@link #getOriginalExceptionType()} and preserves the original exception stacktrace.
 *
 */
public final class WrappedFunctionException extends RuntimeException implements Serializable {
    private final Class<?> originalExceptionType;

    public WrappedFunctionException(Throwable cause){
        super(cause.getMessage());
        this.setStackTrace(cause.getStackTrace());
        this.originalExceptionType = cause.getClass();
    }

    /**
     * Exposes the type of the original error
     * @return the class of the opriginal exception type;
     */
    public Class<?> getOriginalExceptionType() {
        return originalExceptionType;
    }
}
