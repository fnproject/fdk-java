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

package com.fnproject.fn.api.exception;

/**
 * This used to wrap any exception thrown by an {@link com.fnproject.fn.api.InputCoercion}. It is
 * also thrown if no {@link com.fnproject.fn.api.InputCoercion} is applicable to a parameter of the user function.
 *
 * This indicates that the input was not appropriate to this function.
 */
public class FunctionInputHandlingException extends RuntimeException {
    public FunctionInputHandlingException(String s, Throwable t) {
        super(s,t);
    }

    public FunctionInputHandlingException(String s) {
        super(s);
    }
}
