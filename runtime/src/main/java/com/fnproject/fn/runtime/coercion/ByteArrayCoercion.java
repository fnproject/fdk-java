package com.fnproject.fn.runtime.coercion;

import com.fnproject.fn.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Handles coercion to and from byte arrays.
 */
public class ByteArrayCoercion implements InputCoercion<byte[]>, OutputCoercion {
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        if (method.getReturnType().getParameterClass().equals(byte[].class)) {
            return Optional.of(OutputEvent.fromBytes(((byte[]) value), OutputEvent.Status.Success, "application/octet-stream"));
        } else {
            return Optional.empty();
        }
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    @Override
    public Optional<byte[]> tryCoerceParam(InvocationContext currentContext, int arg, InputEvent input, MethodWrapper method) {
        if (method.getParamType(arg).getParameterClass().equals(byte[].class)) {
            return Optional.of(
                    input.consumeBody(is -> {
                        try {
                            return toByteArray(is);
                        } catch (IOException e) {
                            throw new RuntimeException("Error reading input as bytes", e);
                        }
                    }));
        } else {
            return Optional.empty();
        }
    }
}
