package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.Util.hasEventFnInHierarchy;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;


public class ConnectorHubCoercion implements InputCoercion<ConnectorHubBatch<?>> {

    private static final ConnectorHubCoercion instance = new ConnectorHubCoercion();
    static final String OM_KEY = ConnectorHubCoercion.class.getCanonicalName() + ".om";

    public static ConnectorHubCoercion instance() {
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
    public Optional<ConnectorHubBatch<?>> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {
        if (hasEventFnInHierarchy(method.getTargetClass().getSuperclass(), ConnectorHubFunction.class)) {
            Type type = method.getTargetMethod().getGenericParameterTypes()[param];
            JavaType javaType = objectMapper(currentContext).constructType(type);
            List<JavaType> requestGenerics = javaType.getBindings().getTypeParameters();

            ObjectMapper mapper = objectMapper(currentContext);

            JavaType elementType = requestGenerics.get(0);
            JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, elementType);

            List<Object> batchedItems = input.consumeBody(is -> {
                try {
                    return mapper.readValue(is, listType);
                } catch (IOException e) {
                    throw coercionFailed(listType, e);
                }
            });

            return Optional.of(new ConnectorHubBatch(batchedItems, currentContext.getRequestHeaders()));
        } else {
            return Optional.empty();
        }
    }

    private static RuntimeException coercionFailed(Type paramType, Throwable cause) {
        return new RuntimeException("Failed to coerce event to user function parameter type " + paramType, cause);
    }

}