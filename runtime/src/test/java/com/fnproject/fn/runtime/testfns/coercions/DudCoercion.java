package com.fnproject.fn.runtime.testfns.coercions;

import com.fnproject.fn.api.*;

import java.util.Optional;

public class DudCoercion implements InputCoercion<Object>, OutputCoercion {

    @Override
    public Optional<Object> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper methodWrapper) {
        return Optional.empty();
    }

    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        return Optional.empty();
    }
}
