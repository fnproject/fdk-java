package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.Util.hasEventFnInHierarchy;
import static com.fnproject.events.coercion.Util.readBodyAsString;
import static com.fnproject.fn.api.OutputEvent.CONTENT_TYPE_HEADER;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.mapper.APIGatewayRequestEventMapper;
import com.fnproject.events.mapper.ApiGatewayRequestMapper;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputCoercion;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.httpgateway.FunctionHTTPGatewayContext;


public class APIGatewayCoercion implements InputCoercion<APIGatewayRequestEvent>, OutputCoercion {

    private static final APIGatewayCoercion instance = new APIGatewayCoercion();
    static final String OM_KEY = APIGatewayCoercion.class.getCanonicalName() + ".om";
    private final ApiGatewayRequestMapper requestMapper;

    public APIGatewayCoercion(ApiGatewayRequestMapper requestMapper) {
        this.requestMapper = requestMapper;
    }

    private APIGatewayCoercion() {
        requestMapper = new APIGatewayRequestEventMapper();
    }

    public static APIGatewayCoercion instance() {
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
    public Optional<APIGatewayRequestEvent> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {
        if (hasEventFnInHierarchy(method.getTargetClass().getSuperclass(), APIGatewayFunction.class)) {
            FunctionHTTPGatewayContext functionHTTPGatewayContext = new FunctionHTTPGatewayContext(currentContext);

            Type type = method.getTargetMethod().getGenericParameterTypes()[param];
            JavaType javaType = objectMapper(currentContext).constructType(type);
            List<JavaType> requestGenerics = javaType.getBindings().getTypeParameters();

            Object body;

            if (!requestGenerics.isEmpty()) {
                JavaType requestGeneric = requestGenerics.get(0);
                if (requestGeneric.hasRawClass(String.class)) {
                    body = readBodyAsString(input);
                } else {
                    ObjectMapper mapper = objectMapper(currentContext);
                    body = input.consumeBody(is -> {
                        try {
                            return mapper.readValue(is, requestGeneric);
                        } catch (IOException e) {
                            throw coercionFailed(requestGeneric, e);
                        }
                    });
                }
            } else {
                body = readBodyAsString(input);
            }
            APIGatewayRequestEvent requestEvent = requestMapper.toApiGatewayRequestEvent(functionHTTPGatewayContext, body);
            return Optional.of(requestEvent);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext currentContext, MethodWrapper method, Object value) {
        if (!hasEventFnInHierarchy(method.getTargetClass().getSuperclass(), APIGatewayFunction.class) || value == null) {
            return Optional.empty();
        }

        FunctionHTTPGatewayContext ctx = new FunctionHTTPGatewayContext(currentContext);
        APIGatewayResponseEvent responseEvent = (APIGatewayResponseEvent) value;

        String contentType = null;

        if (responseEvent.getHeaders() != null) {
            for (String key : responseEvent.getHeaders().asMap().keySet()) {
                ctx.addResponseHeader(key, responseEvent.getHeaders().getAllValues(key));
            }

            Optional<String> userSetContentType = responseEvent.getHeaders().get(CONTENT_TYPE_HEADER);
            if (userSetContentType.isPresent()) {
                contentType = userSetContentType.get();
            }
        }

        if (responseEvent.getStatus() != null) {
            ctx.setStatusCode(responseEvent.getStatus());
        }

        Optional<Object> body = Optional.ofNullable(responseEvent.getBody());

        Type gs = method.getTargetClass().getGenericSuperclass();
        if (gs instanceof ParameterizedType) {
            Type responseGeneric = ((ParameterizedType) gs).getActualTypeArguments()[1]; // response type param
            JavaType responseType = TypeFactory
                .defaultInstance()
                .constructType(responseGeneric);

            if (responseType.hasRawClass(String.class)) {
                if (contentType == null) {
                    contentType = "text/plain";
                }
                return Optional.of(OutputEvent.fromBytes(((String) body.orElse("")).getBytes(), OutputEvent.Status.Success, contentType));
            }
            if (contentType == null) {
                contentType = "application/json";
            }

            try {
                return Optional.of(OutputEvent.fromBytes(objectMapper(currentContext).writeValueAsBytes(body.orElse("")), OutputEvent.Status.Success, contentType));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to render response to JSON", e);
            }
        } else {
            if (contentType == null) {
                contentType = "text/plain";
            }
            return Optional.of(OutputEvent.fromBytes(((String) body.orElse("")).getBytes(), OutputEvent.Status.Success, contentType));
        }

    }

    private static RuntimeException coercionFailed(Type paramType, Throwable cause) {
        return new RuntimeException("Failed to coerce event to user function parameter type " + paramType, cause);
    }

}