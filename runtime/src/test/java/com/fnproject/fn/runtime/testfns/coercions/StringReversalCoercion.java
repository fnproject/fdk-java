package com.fnproject.fn.runtime.testfns.coercions;

import com.fnproject.fn.api.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class StringReversalCoercion implements InputCoercion<String>, OutputCoercion {
    @Override
    public Optional<String> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper methodWrapper) {
        return Optional.of(
                input.consumeBody(is -> {
                    try {
                        return new StringBuffer(IOUtils.toString(is, StandardCharsets.UTF_8)).reverse().toString();
                    } catch (IOException e) {
                        return null; // Tests will fail if we end up here
                    }
                })
        );
    }

    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        if (ctx.getRuntimeContext().getMethod().getTargetMethod().getReturnType().equals(String.class)) {
            try {
                String reversedOutput = new StringBuffer((String) value).reverse().toString();
                return Optional.of(OutputEvent.fromBytes(reversedOutput.getBytes(), OutputEvent.Status.Success, "text/plain"));
            } catch (ClassCastException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
