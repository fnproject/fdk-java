package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

public class MethodParameter extends DefaultMethodType {
    private int index;

    public MethodParameter(MethodWrapper src, int index) {
        super(src, resolveType(src.getTargetMethod().getGenericParameterTypes()[index], src));
        this.index = index;
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }
}
