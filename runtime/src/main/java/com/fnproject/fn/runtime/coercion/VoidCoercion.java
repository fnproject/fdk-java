package com.fnproject.fn.runtime.coercion;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputCoercion;
import com.fnproject.fn.api.OutputEvent;

import java.util.Optional;

public class VoidCoercion implements OutputCoercion {
    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        if (method.getReturnType().getParameterClass().equals(Void.class)) {
            return Optional.of(OutputEvent.emptyResult(OutputEvent.SUCCESS));
        } else {
            return Optional.empty();
        }
    }
}

