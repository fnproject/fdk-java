package com.fnproject.fn.runtime.coercion;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.OutputCoercion;
import com.fnproject.fn.api.OutputEvent;

import java.util.Optional;

public class VoidCoercion implements OutputCoercion {
    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, Object value) {
        if (ctx.getRuntimeContext().getTargetMethod().getReturnType().equals(void.class)) {
            return Optional.of(OutputEvent.emptyResult(true));
        } else {
            return Optional.empty();
        }
    }
}
