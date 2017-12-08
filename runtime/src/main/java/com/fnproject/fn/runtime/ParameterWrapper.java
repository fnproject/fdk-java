package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

/**
 * A {@link com.fnproject.fn.api.TypeWrapper} for capturing type information about a method's parameter.
 */
class ParameterWrapper extends MethodTypeWrapper {

    /**
     * Constructor
     *
     * @param method     the method
     * @param paramIndex the index of the parameter which we store type information about
     */
    public ParameterWrapper(MethodWrapper method, int paramIndex) {
        super(method, resolveType(method.getTargetMethod().getGenericParameterTypes()[paramIndex], method));
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }
}
