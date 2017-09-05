package com.fnproject.fn.runtime.coercion;


import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;

import java.util.Optional;

public class InputEventCoercion implements InputCoercion<InputEvent> {

    @Override
    public Optional<InputEvent> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input) {
        if (parameterType(currentContext, arg).equals(InputEvent.class)) {
            return Optional.of(input);
        } else {
            return Optional.empty();
        }
    }

    private Class<?> parameterType(InvocationContext currentContext, int arg) {
        return currentContext.getRuntimeContext().getMethod().param(arg).getParameterClass();
    }
}
