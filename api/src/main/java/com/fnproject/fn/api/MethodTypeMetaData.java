package com.fnproject.fn.api;

import net.jodah.typetools.TypeResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class MethodTypeMetaData {
    protected final MethodWrapper src;
    protected Class<?> parameterClass;

    public MethodTypeMetaData(MethodWrapper src, Class<?> parameterClass) {
        this.src = src;
        this.parameterClass = parameterClass;
    }

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
