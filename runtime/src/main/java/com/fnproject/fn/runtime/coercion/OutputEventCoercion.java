package com.fnproject.fn.runtime.coercion;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputCoercion;
import com.fnproject.fn.api.OutputEvent;

import java.util.Optional;

public class OutputEventCoercion implements OutputCoercion {
    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        if (method.getReturnType().getParameterClass().equals(OutputEvent.class)) {
            return Optional.of((OutputEvent) value);
        } else {
            return Optional.empty();
        }
    }
}
