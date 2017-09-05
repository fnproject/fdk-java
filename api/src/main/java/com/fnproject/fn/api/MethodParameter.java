package com.fnproject.fn.api;

public class MethodParameter extends MethodTypeMetaData {
    private int index;

    public MethodParameter(MethodWrapper src, int index) {
        super(src, resolveType(src.getTargetMethod().getGenericParameterTypes()[index], src));
        this.index = index;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }
}
