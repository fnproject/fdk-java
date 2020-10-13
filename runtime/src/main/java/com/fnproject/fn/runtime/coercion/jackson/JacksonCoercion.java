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

package com.fnproject.fn.runtime.coercion.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Jackson JSON Serialization feature -
 * <p>
 * This supports marshalling and unmarshalling of event parameters and responses to
 */
public class JacksonCoercion implements InputCoercion<Object>, OutputCoercion {
    private static final String OM_KEY = JacksonCoercion.class.getCanonicalName() + ".om";

    private static final JacksonCoercion instance = new JacksonCoercion();

    /**
     * Return the global instance of this coercion
     * @return a singleton instance of the JSON coercion for the VM
     */
    public static JacksonCoercion instance() {
        return instance;
    }

    private static ObjectMapper objectMapper(InvocationContext ctx) {
        Optional<ObjectMapper> omo = ctx.getRuntimeContext().getAttribute(OM_KEY, ObjectMapper.class);
        if (!omo.isPresent()) {
            ObjectMapper om = new ObjectMapper();

            ctx.getRuntimeContext().setAttribute(OM_KEY, om);
            return om;
        } else {
            return omo.get();
        }
    }

    @Override
    public Optional<Object> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {

        Type type = method.getTargetMethod().getGenericParameterTypes()[param];
        JavaType javaType = objectMapper(currentContext).constructType(type);

        return Optional.ofNullable(input.consumeBody(inputStream -> {
            try {
                return objectMapper(currentContext).readValue(inputStream, javaType);
            } catch (IOException e) {
                throw coercionFailed(type, e);
            }
        }));

    }


    private static RuntimeException coercionFailed(Type paramType, Throwable cause) {
        return new RuntimeException("Failed to coerce event to user function parameter type " + paramType, cause);
    }

    private static RuntimeException coercionFailed(Type paramType) {
        return coercionFailed(paramType, null);
    }

    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {

        try {
            return Optional.of(OutputEvent.fromBytes(objectMapper(ctx).writeValueAsBytes(value), OutputEvent.Status.Success,
                    "application/json"));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to render response to JSON", e);
        }

    }

}
