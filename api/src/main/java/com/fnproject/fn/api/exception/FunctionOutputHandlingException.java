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
 * This used to wrap any exception thrown by an {@link com.fnproject.fn.api.OutputCoercion}. It is
 * also thrown if no {@link com.fnproject.fn.api.OutputCoercion} is applicable to the object returned by the function.
 */
public class FunctionOutputHandlingException extends RuntimeException {
    public FunctionOutputHandlingException(String s, Exception e) {
        super(s,e);
    }

    public FunctionOutputHandlingException(String s) {
        super(s);

    }
}
