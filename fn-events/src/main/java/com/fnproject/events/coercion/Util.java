package com.fnproject.events.coercion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fnproject.fn.api.InputEvent;
import org.apache.commons.io.IOUtils;

public class Util {
    public static boolean hasEventFnInHierarchy(Class<?> targetClass, Class<?> eventClass) {
        for (Class<?> c = targetClass; c != null; c = c.getSuperclass()) {
            if (c == eventClass) return true;
        }
        return false;
    }

    static String readBodyAsString(InputEvent input) {
        return input.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string", e);
            }
        });
    }
}
