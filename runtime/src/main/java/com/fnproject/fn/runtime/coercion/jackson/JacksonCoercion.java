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
    private static String OM_KEY = JacksonCoercion.class.getCanonicalName() + ".om";

    private static JacksonCoercion instance = new JacksonCoercion();

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
            return Optional.of(OutputEvent.fromBytes(objectMapper(ctx).writeValueAsBytes(value), OutputEvent.SUCCESS,
                    "application/json"));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to render response to JSON", e);
        }

    }

}
