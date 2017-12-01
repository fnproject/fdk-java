package com.fnproject.springframework.function;

import com.fnproject.fn.api.TypeWrapper;

/**
 * Implementation of @{@link TypeWrapper} which stores the type
 * passed in directly.
 */
public class SimpleTypeWrapper implements TypeWrapper {
    private final Class<?> cls;

    public SimpleTypeWrapper(Class<?> cls) {
        this.cls = cls;
    }

    @Override
    public Class<?> getParameterClass() {
        return cls;
    }
}
