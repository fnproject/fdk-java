package com.fnproject.fn.runtime.spring.function;

import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.fn.runtime.DefaultTypeWrapper;
import org.springframework.cloud.function.context.FunctionInspector;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class SpringCloudConsumer extends SpringCloudMethod {
    private Consumer<Flux<?>> consumer;

    public SpringCloudConsumer(Consumer<Flux<?>> consumer, FunctionInspector inspector) {
        super(inspector);
        this.consumer = consumer;
    }

    @Override
    protected String getMethodName() {
        return "accept";
    }

    @Override
    protected Object getFunction() {
        return consumer;
    }

    @Override
    public TypeWrapper getReturnType() {
        return new DefaultTypeWrapper(Void.class);
    }
    @Override
    public Flux<?> invoke(Flux<?> arg) {
        consumer.accept(arg);
        return Flux.empty();
    }

}
