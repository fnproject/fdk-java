package com.fnproject.fn.api;

import java.util.Optional;

public interface OutputCoercion {
    Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value);
}
