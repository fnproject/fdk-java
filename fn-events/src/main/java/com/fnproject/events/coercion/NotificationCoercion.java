package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.Util.hasEventFnInHierarchy;
import static com.fnproject.events.coercion.Util.readBodyAsString;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.NotificationFunction;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;


public class NotificationCoercion implements InputCoercion<NotificationMessage<?>> {

    private static final NotificationCoercion instance = new NotificationCoercion();
    static final String OM_KEY = NotificationCoercion.class.getCanonicalName() + ".om";

    public static NotificationCoercion instance() {
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
    public Optional<NotificationMessage<?>> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {
        if (hasEventFnInHierarchy(method.getTargetClass().getSuperclass(), NotificationFunction.class)) {
            Type type = method.getTargetMethod().getGenericParameterTypes()[param];
            JavaType javaType = objectMapper(currentContext).constructType(type);
            List<JavaType> requestGenerics = javaType.getBindings().getTypeParameters();

            JavaType elementType = requestGenerics.get(0);
            if (elementType.hasRawClass(String.class)) {
                return Optional.of(new NotificationMessage(readBodyAsString(input), currentContext.getRequestHeaders()));
            }

            ObjectMapper mapper = objectMapper(currentContext);
            Object messageContent = input.consumeBody(is -> {
                try {
                    return mapper.readValue(is, elementType);
                } catch (IOException e) {
                    throw coercionFailed(elementType, e);
                }
            });

            return Optional.of(new NotificationMessage<>(messageContent, currentContext.getRequestHeaders()));
        } else {
            return Optional.empty();
        }
    }

    private static RuntimeException coercionFailed(Type paramType, Throwable cause) {
        return new RuntimeException("Failed to coerce event to user function parameter type " + paramType, cause);
    }

}