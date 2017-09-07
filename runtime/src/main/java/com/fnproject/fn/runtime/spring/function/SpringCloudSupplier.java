package com.fnproject.fn.runtime.spring.function;

import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.fn.runtime.DefaultTypeWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

public class SpringCloudSupplier extends SpringCloudMethod {
    private Supplier<Flux<?>> supplier;

    public SpringCloudSupplier(Supplier<Flux<?>> supplier, FunctionInspector inspector) {
        super(inspector);
        this.supplier = supplier;
    }

    @Override
    protected String getMethodName() {
        return "get";
    }

    @Override
    protected Object getFunction() {
        return supplier;
    }

    @Override
    public TypeWrapper getParamType(int i) {
        return new DefaultTypeWrapper(Void.class);
    }

    @Override
    public Flux<?> invoke(Flux<?> arg) {
        return supplier.get();
    }
}
