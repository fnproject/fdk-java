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

/**
 * Exception thrown when the completion facility fails to operate on a completion graph
 */
public class PlatformException extends FlowCompletionException {
    public PlatformException(Throwable t) {
        super(t);
    }
    public PlatformException(String message) {
        super(message);
    }
    public PlatformException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * These are manufactured exceptions that arise outside the current runtime; therefore,
     * the notion of an embedded stack trace is meaningless.
     *
     * @return this
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
