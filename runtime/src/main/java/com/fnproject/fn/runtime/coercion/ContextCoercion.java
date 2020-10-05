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
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.fnproject.fn.runtime.httpgateway.FunctionHTTPGatewayContext;

import java.util.Optional;

/**
 * Handles coercion to build in context objects ({@link RuntimeContext}, {@link InvocationContext} , {@link HTTPGatewayContext})
 */
public class ContextCoercion implements InputCoercion<Object> {

    @Override
    public Optional<Object> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper method) {
        Class<?> paramClass = method.getParamType(arg).getParameterClass();

        if (paramClass.equals(RuntimeContext.class)) {
            return Optional.of(currentContext.getRuntimeContext());
        } else if (paramClass.equals(InvocationContext.class)) {
            return Optional.of(currentContext);
        } else if (paramClass.equals(HTTPGatewayContext.class)) {
            return Optional.of(new FunctionHTTPGatewayContext(currentContext));
        } else {
            return Optional.empty();
        }
    }
}
