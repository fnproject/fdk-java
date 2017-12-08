package com.fnproject.springframework.function;

import net.jodah.typetools.TypeResolver;
import org.springframework.cloud.function.context.FunctionInspector;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleFunctionInspector implements FunctionInspector {
    @Override
    public boolean isMessage(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Class<?> getInputType(Object function) {
        if (function instanceof Function) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            return types[0];
        } else if (function instanceof Consumer) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Consumer.class, function.getClass());
            return types[0];
        } else if (function instanceof Supplier) {
            return Void.class;
        } else {
            throw new IllegalStateException("You cannot get the input type of a function that doesn't implement one of the java.util.function interfaces");
        }
    }

    @Override
    public Class<?> getOutputType(Object function) {
        if (function instanceof Function) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            return types[0];
        } else if (function instanceof Consumer) {
            return Void.class;
        } else if (function instanceof Supplier) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Consumer.class, function.getClass());
            return types[0];
        } else {
            throw new IllegalStateException("You cannot get the output type of a function that doesn't implement one of the java.util.function interfaces");
        }
    }

    @Override
    public Class<?> getInputWrapper(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Class<?> getOutputWrapper(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Object convert(Object function, String value) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getName(Object function) {
        return function.toString();
    }
}
