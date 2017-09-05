package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodType;
import com.fnproject.fn.api.MethodWrapper;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Wrapper class around {@link java.lang.reflect.Method} to provide type
 * resolution for methods with generic arguments
 */
public class DefaultMethodWrapper implements MethodWrapper {
    private final Class<?> srcClass;
    private final Method srcMethod;

    DefaultMethodWrapper(Class<?> srcClass, Method srcMethod) {
        this.srcClass = srcClass;
        this.srcMethod = srcMethod;
    }

    public DefaultMethodWrapper(Class<?> srcClass, String srcMethod) {
        this.srcClass = srcClass;
        this.srcMethod = Arrays.stream(srcClass.getMethods())
                .filter((m) -> m.getName().equals(srcMethod))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(new NoSuchMethodException(srcClass.getCanonicalName() + "::" + srcMethod)));
    }


    @Override
    public Class<?> getTargetClass() {
        return srcClass;
    }

    @Override
    public Method getTargetMethod() {
        return srcMethod;
    }

    @Override
    public MethodType getParamType(int index) {
        return new MethodParameter(this, index);
    }

    @Override
    public MethodType getReturnType() {
        return new MethodReturnType(this);
    }

    @Override
    public String toString() {
        return getLongName();
    }
}

