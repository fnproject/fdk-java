package com.fnproject.fn.runtime.spring.function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Function;

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
