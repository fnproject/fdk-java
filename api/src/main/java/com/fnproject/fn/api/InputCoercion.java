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
 * Handles the coercion of an input event to a parameter
 */
public interface InputCoercion<T> {
    /**
     * Handle coercion for a function parameter
     * <p>
     * Coercions may choose to act on a parameter, in which case they should return a fulfilled option) or may
     * ignore parameters (allowing other coercions to act on the parameter)
     * <p>
     * When a coercion ignores a parameter it must not consume the input stream of the event.
     * <p>
     * If a coercion throws a RuntimeException, no further coercions will be tried and the function invocation will fail.
     *
     * @param currentContext the invocation context for this event - this links to the {@link RuntimeContext} and method and class
     * @param arg            the parameter index for the argument being extracted
     * @param input          the input event
     * @param methodWrapper  the method which the parameter is for
     * @return               the result of the coercion, if it succeeded
     */
    Optional<T> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper methodWrapper);

}
