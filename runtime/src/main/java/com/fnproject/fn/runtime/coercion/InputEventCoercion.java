package com.fnproject.fn.runtime.coercion;


import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;

import java.util.Optional;

public class InputEventCoercion implements InputCoercion<InputEvent> {

    @Override
    public Optional<InputEvent> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper method) {
        if (method.getParamType(arg).getParameterClass().equals(InputEvent.class)) {
            return Optional.of(input);
        } else {
            return Optional.empty();
        }
    }
}
