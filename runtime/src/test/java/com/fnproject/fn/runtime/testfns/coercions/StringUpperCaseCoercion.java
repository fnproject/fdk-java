package com.fnproject.fn.runtime.testfns.coercions;

import com.fnproject.fn.api.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class StringUpperCaseCoercion implements InputCoercion<String>, OutputCoercion {

    @Override
    public Optional<String> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper methodWrapper) {
        return Optional.of(
                input.consumeBody(is -> {
                    try {
                        return new StringBuffer(IOUtils.toString(is, StandardCharsets.UTF_8)).toString().toUpperCase();
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
                String capitilisedOutput = new StringBuffer((String) value).toString().toUpperCase();
                return Optional.of(OutputEvent.fromBytes(capitilisedOutput.getBytes(), OutputEvent.SUCCESS, "text/plain"));
            } catch (ClassCastException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
