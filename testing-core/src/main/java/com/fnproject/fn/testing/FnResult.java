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

package com.fnproject.fn.testing;

import com.fnproject.fn.api.OutputEvent;

/**
 * A simple abstraction over {@link OutputEvent} that buffers the response body
 */
public interface FnResult  extends OutputEvent {
    /**
     * Returns the body of the function result as a byte array
     *
     * @return the function response body
     */
    byte[] getBodyAsBytes();

    /**
     * Returns the body of the function response as a string
     *
     * @return a function response body
     */
    String getBodyAsString();


    /**
     * Determine if the status code corresponds to a successful invocation
     *
     * @return true if the status code indicates success
     */
    default boolean isSuccess() {
        return getStatus() == Status.Success;
    }
}
