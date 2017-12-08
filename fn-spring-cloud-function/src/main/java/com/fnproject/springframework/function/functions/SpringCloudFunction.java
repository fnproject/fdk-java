package com.fnproject.springframework.function.functions;

import org.springframework.cloud.function.context.FunctionInspector;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link SpringCloudMethod} representing a {@link Function}
 */
public class SpringCloudFunction extends SpringCloudMethod {
    private Function<Flux<?>, Flux<?>> function;

    public SpringCloudFunction(Function<Flux<?>, Flux<?>> function, FunctionInspector inspector) {
        super(inspector);
        this.function = function;
    }

    @Override
    protected String getMethodName() {
        return "apply";
    }

    @Override
    protected Object getFunction() {
        return function;
    }

    @Override
    public Flux<?> invoke(Flux<?> arg) {
        return function.apply(arg);
    }
}
