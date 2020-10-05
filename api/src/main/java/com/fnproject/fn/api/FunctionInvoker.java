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

package com.fnproject.fn.api;

import java.util.Optional;

/**
 * Handles the invocation of a given function call
 */
public interface FunctionInvoker {
    /**
     * Phase determines a loose ordering for invocation handler processing
     * this should be used with {@link RuntimeContext#addInvoker(FunctionInvoker, Phase)} to add new invoke handlers to a runtime
     */
    enum Phase {
        /**
         * The Pre-Call phase runs before the main function call, all {@link FunctionInvoker} handlers added at this phase are tried prior to calling the {@link Phase#Call} phase
         * This phase is typically used for handlers that /may/ intercept the request based on request attributes
         */
        PreCall,
        /**
         * The Call Phase indicates invokers that should handle call values - typically a given runtime will only be handled by one of these
         */
        Call
    }

    /**
     * Optionally handles an invocation chain for this function
     * <p>
     * If the invoker returns an empty option then no action has been taken and another invoker may attempt to tryInvoke
     * <p>
     * In particular this means that implementations must not read the input event body until they are committed to handling this event.
     *
     * A RuntimeException thrown by the implementation will cause the entire function invocation to fail.
     *
     * @param ctx the context for a single invocation
     * @param evt the incoming event
     * @return a optional output event if the invoker handled the event.
     */
    Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt);
}
