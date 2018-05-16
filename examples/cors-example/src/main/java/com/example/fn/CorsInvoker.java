package com.example.fn;

import com.fnproject.fn.api.*;

import java.util.Optional;

/**
 *
 * A simple CORS interceptor - this handles OPTIONS requests to functions and
 * returns some canned CORS headeers.
 *
 * Created on 16/05/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
class CorsInvoker implements FunctionInvoker {
    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext invocationContext, InputEvent inputEvent) {
        // skip options calls, this uses the headers in func.yaml to set cors headers
        if (inputEvent.getMethod().equals("OPTIONS")) {
            return Optional.of(
               OutputEvent.fromBytes(new byte[0],
                  200,
                  "application/json",
                  Headers.emptyHeaders()));
        }
        return Optional.empty();
    }
}
