package com.fnproject.fn.testing;

import com.oracle.completer.actor.domain.CompleterDomain;
import com.oracle.completer.actor.messages.ResultHelpers;
import com.oracle.completer.io.SerUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

class FunctionInvocation implements Invocation {
    private final CompletableFuture<CompleterDomain.CompletionResult> output;
    private final String functionId;
    private byte[] body;

    FunctionInvocation(String functionId, byte[] body, CompletableFuture<CompleterDomain.CompletionResult> output) {
        this.functionId = functionId;
        this.body = body;
        this.output = output;
    }

    public void invoke(Invocation.PlatformSimulator simulator) {
        try {
            try {
                byte[] result = simulator.invokeExternalFunction(functionId, body);
                output.complete(ResultHelpers.resultFromMagicBytes(SerUtils.serialize(result)));
            } catch (FunctionError functionError) {
                output.complete(ResultHelpers.failedFromUserException(functionError));
            } catch (PlatformError platformError) {
                output.complete(ResultHelpers.failedFromPlatformException(platformError));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new Error("Testing framework error: Could not serialise function result.");
        }
    }
}
