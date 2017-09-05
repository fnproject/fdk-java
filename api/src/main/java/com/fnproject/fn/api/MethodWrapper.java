package com.fnproject.fn.api;

import java.lang.reflect.Method;

/**
 * Wrapper class around {@link java.lang.reflect.Method} to provide type
 * resolution for methods with generic arguments
 */
public class MethodWrapper {
    private final Class<?> srcClass;
    private final Method srcMethod;
    private String canonicalName;

    public MethodWrapper(Class<?> srcClass, Method srcMethod) {
        this.srcClass = srcClass;
        this.srcMethod = srcMethod;
    }

    public Class<?> getTargetClass() {
        return srcClass;
    }

    public Method getTargetMethod() {
        return srcMethod;
    }

    public MethodParameter param(int index) {
        return new MethodParameter(this, index);
    }

    public MethodReturnType getReturnType() {
        return new MethodReturnType(this);
    }

    public String getCanonicalName() {
        return getTargetClass().getCanonicalName() + "::" + getTargetMethod().getName();
    }
}
