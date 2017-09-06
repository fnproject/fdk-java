package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodType;
import com.fnproject.fn.api.MethodWrapper;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class DefaultMethodType implements MethodType {
    protected final MethodWrapper src;
    protected Class<?> parameterClass;

    public DefaultMethodType(MethodWrapper src, Class<?> parameterClass) {
        this.src = src;
        this.parameterClass = parameterClass;
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }

    protected static Class<?> resolveType(Type type, MethodWrapper src) {
        if (type instanceof Class) {
            return TypeWrapper.resolvePrimitiveType((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        Class<?> resolvedType = TypeResolver.resolveRawArgument(type, src.getTargetClass());
        if (resolvedType == TypeResolver.Unknown.class) {
            // TODO: Decide what exception to throw here
            throw new RuntimeException("Cannot infer type of method parameter");
        } else {
            return resolvedType;
        }
    }

}
