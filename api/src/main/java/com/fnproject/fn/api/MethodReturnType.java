package com.fnproject.fn.api;

public class MethodReturnType extends MethodTypeMetaData {

    public MethodReturnType(MethodWrapper src) {
        super(src, resolveType(src.getTargetMethod().getGenericReturnType(), src));
    }

}
