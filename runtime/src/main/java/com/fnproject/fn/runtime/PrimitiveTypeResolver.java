package com.fnproject.fn.runtime;

import java.util.HashMap;
import java.util.Map;

public class PrimitiveTypeResolver {
    private static Map<Class<?>, Class<?>> boxedTypes = new HashMap<>();
    static {
        boxedTypes.put(void.class, Void.class);
        boxedTypes.put(boolean.class, Boolean.class);
        boxedTypes.put(byte.class, Byte.class);
        boxedTypes.put(char.class, Character.class);
        boxedTypes.put(short.class, Short.class);
        boxedTypes.put(int.class, Integer.class);
        boxedTypes.put(long.class, Long.class);
        boxedTypes.put(float.class, Float.class);
        boxedTypes.put(double.class, Double.class);
    }

    /**
     * Resolves cls from a possibly primitive class to a boxed type otherwise just returns cls
     */
    public static Class<?> resolve(Class<?> cls) {
        if (cls.isPrimitive()) {
            return boxedTypes.get(cls);
        }
        return cls;
    }
}
