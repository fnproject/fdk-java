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
            return Optional.of(OutputEvent.fromBytes(((String) value).getBytes(), OutputEvent.SUCCESS, "text/plain"));
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
