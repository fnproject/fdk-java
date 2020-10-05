/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
