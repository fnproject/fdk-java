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
 * Exception thrown when a lambda or any referenced objects fail to be serialized.
 * The cause will typically be a {@link java.io.NotSerializableException} or other {@link java.io.IOException} detailing what could not be serialized
 */
public class LambdaSerializationException extends FlowCompletionException {
    public LambdaSerializationException(String message) {
        super(message);
    }

    public LambdaSerializationException(String message, Exception e) {
        super(message, e);
    }
}
