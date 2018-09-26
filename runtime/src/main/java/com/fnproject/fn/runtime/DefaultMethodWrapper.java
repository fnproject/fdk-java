package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.TypeWrapper;

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

    DefaultMethodWrapper(Class<?> srcClass, String srcMethod) {
        this(srcClass, Arrays.stream(srcClass.getMethods())
          .filter((m) -> m.getName().equals(srcMethod))
          .findFirst()
          .orElseThrow(() -> new RuntimeException(new NoSuchMethodException(srcClass.getCanonicalName() + "::" + srcMethod))));
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
    public TypeWrapper getParamType(int index) {
        return MethodTypeWrapper.fromParameter(this, index);
    }

    @Override
    public TypeWrapper getReturnType() {
        return MethodTypeWrapper.fromReturnType(this);
    }

    @Override
    public String toString() {
        return getLongName();
    }
}

