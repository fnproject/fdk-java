package com.fnproject.fn.runtime;

import com.fnproject.fn.api.TypeWrapper;

public class DefaultTypeWrapper implements TypeWrapper {
    private final Class<?> cls;

    public DefaultTypeWrapper(Class<?> cls) {
        this.cls = cls;
    }

    @Override
    public Class<?> getParameterClass() {
        return cls;
    }
}