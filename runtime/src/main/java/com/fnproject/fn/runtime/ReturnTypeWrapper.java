package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

class ReturnTypeWrapper extends MethodTypeWrapper {

    ReturnTypeWrapper(MethodWrapper src) {
        super(src, resolveType(src.getTargetMethod().getGenericReturnType(), src));
    }

}
