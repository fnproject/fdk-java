package com.fnproject.fn.runtime.spring.function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

public class SpringCloudSupplier extends SpringCloudMethod {
    private Supplier<Object> supplier;

    public SpringCloudSupplier(Supplier<Object> supplier, FunctionInspector inspector) {
        super(inspector);
        this.supplier = supplier;
    }

    @Override
    protected String getMethodName() {
        return "apply";
    }

    @Override
    protected Object getFunction() {
        return supplier;
    }

    @Override
    public Flux<?> invoke(Object... userFunctionParams) {
        return Flux.just(supplier.get());
    }
}
