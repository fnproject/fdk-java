package com.fnproject.fn.runtime.spring.function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class SpringCloudConsumer extends SpringCloudMethod {
    private Consumer<Object> consumer;

    SpringCloudConsumer(Consumer<Object> consumer, FunctionInspector inspector) {
        super(inspector);
        this.consumer = consumer;
    }

    @Override
    protected String getMethodName() {
        return "apply";
    }

    @Override
    protected Object getFunction() {
        return consumer;
    }

    @Override
    public Flux<?> invoke(Object... userFunctionParams) {
        consumer.accept(userFunctionParams[0]);
        return Flux.empty();
    }

}
