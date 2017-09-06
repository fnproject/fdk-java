package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

class MethodParameter extends DefaultMethodType {
    private int index;

    MethodParameter(MethodWrapper src, int index) {
        super(src, resolveType(src.getTargetMethod().getGenericParameterTypes()[index], src));
        this.index = index;
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }
}
