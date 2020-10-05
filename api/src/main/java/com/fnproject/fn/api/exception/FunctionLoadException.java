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
 * an exception relating to loading the users code into runtime
 * see children for specific cases
 */
public abstract class FunctionLoadException extends RuntimeException {

    public FunctionLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionLoadException(String msg) {
        super(msg);
    }
}
