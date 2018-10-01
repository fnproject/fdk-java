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
