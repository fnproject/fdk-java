package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

class MethodReturnType extends DefaultMethodType {

    MethodReturnType(MethodWrapper src) {
        super(src, resolveType(src.getTargetMethod().getGenericReturnType(), src));
    }

}
