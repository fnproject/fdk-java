package com.fnproject.springframework.function.functions;

import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.springframework.function.SimpleTypeWrapper;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

/**
 * {@link SpringCloudMethod} representing a {@link Supplier}
 */
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
        return new SimpleTypeWrapper(Void.class);
    }

    @Override
    public Flux<?> invoke(Flux<?> arg) {
        return supplier.get();
    }
}
