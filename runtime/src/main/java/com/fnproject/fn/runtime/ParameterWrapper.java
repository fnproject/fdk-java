package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

class ParameterWrapper extends MethodTypeWrapper {
    private int index;

    ParameterWrapper(MethodWrapper src, int index) {
        super(src, resolveType(src.getTargetMethod().getGenericParameterTypes()[index], src));
        this.index = index;
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }
}
