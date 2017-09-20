package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

class ParameterWrapper extends MethodTypeWrapper {

    ParameterWrapper(MethodWrapper src, int index) {
        super(src, resolveType(src.getTargetMethod().getGenericParameterTypes()[index], src));
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }
}