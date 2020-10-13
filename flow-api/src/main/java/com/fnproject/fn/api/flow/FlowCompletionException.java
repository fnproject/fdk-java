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
 * Exception thrown when a blocking operation on a flow fails - this corresponds to a
 * {@link java.util.concurrent.CompletionException} in {@link java.util.concurrent.CompletableFuture} calls
 */
public class FlowCompletionException extends RuntimeException {

    /**
     * If an exception was raised from within a stage, this will be the wrapped cause.
     * @param t  The user exception
     */
    public FlowCompletionException(Throwable t) {
        super(t);
    }

    public FlowCompletionException(String message) {
        super(message);
    }

    public FlowCompletionException(String message, Throwable t) {
        super(message, t);
    }

}
