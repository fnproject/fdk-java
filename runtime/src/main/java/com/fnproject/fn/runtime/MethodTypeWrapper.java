package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.TypeWrapper;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class MethodTypeWrapper implements TypeWrapper {
    private final Class<?> parameterClass;

    private MethodTypeWrapper(Class<?> parameterClass) {
        this.parameterClass = parameterClass;
    }

    @Override
    public Class<?> getParameterClass() {
        return parameterClass;
    }

    static Class<?> resolveType(Type type, MethodWrapper src) {
        if (type instanceof Class) {
            return PrimitiveTypeResolver.resolve((Class<?>) type);
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

    public  static TypeWrapper fromParameter(MethodWrapper method, int paramIndex) {
        return new MethodTypeWrapper(resolveType(method.getTargetMethod().getGenericParameterTypes()[paramIndex], method));
    }

    public static TypeWrapper fromReturnType(MethodWrapper method) {
        return new MethodTypeWrapper(resolveType(method.getTargetMethod().getGenericReturnType(), method));

    }

}
