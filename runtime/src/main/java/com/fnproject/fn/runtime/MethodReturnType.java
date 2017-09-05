package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

public class MethodReturnType extends DefaultMethodType {

    public MethodReturnType(MethodWrapper src) {
        super(src, resolveType(src.getTargetMethod().getGenericReturnType(), src));
    }

}
