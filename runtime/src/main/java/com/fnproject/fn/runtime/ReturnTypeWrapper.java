package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

/**
 * A {@link com.fnproject.fn.api.TypeWrapper} for capturing type information about a method's parameter.
 */
class ReturnTypeWrapper extends MethodTypeWrapper {

    /**
     * Constructor
     *
     * @param method     the method which we store return-type information about
     */
    ReturnTypeWrapper(MethodWrapper method) {
        super(method, resolveType(method.getTargetMethod().getGenericReturnType(), method));
    }
}
