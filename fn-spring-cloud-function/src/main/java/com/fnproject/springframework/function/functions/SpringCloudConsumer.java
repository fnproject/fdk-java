package com.fnproject.springframework.function.functions;

import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.springframework.function.SimpleTypeWrapper;
import org.springframework.cloud.function.context.FunctionInspector;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * {@link SpringCloudMethod} representing a {@link Consumer}
 */
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
        return new SimpleTypeWrapper(Void.class);
    }
    @Override
    public Flux<?> invoke(Flux<?> arg) {
        consumer.accept(arg);
        return Flux.empty();
    }

}
