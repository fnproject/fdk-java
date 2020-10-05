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

package com.fnproject.fn.runtime.coercion;

import com.fnproject.fn.api.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class StringCoercion implements InputCoercion<String>, OutputCoercion {
    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        if (method.getReturnType().getParameterClass().equals(String.class)) {
            return Optional.of(OutputEvent.fromBytes(((String) value).getBytes(), OutputEvent.Status.Success, "text/plain"));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {
        if (method.getParamType(param).getParameterClass().equals(String.class)) {
            return Optional.of(
                    input.consumeBody(is -> {
                        try {
                            return IOUtils.toString(is, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new RuntimeException("Error reading input as string");
                        }
                    }));
        } else {
            return Optional.empty();
        }
    }
}
