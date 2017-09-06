package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.runtime.spring.function.SpringCloudFunction;
import com.fnproject.fn.runtime.spring.function.SpringCloudMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.cloud.function.registry.FunctionCatalog;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringCloudFunctionLoader {
    private final FunctionCatalog catalog;
    private final FunctionInspector inspector;

    private Function<Object, Object> function;
    private Consumer<Object> consumer;
    private Supplier<Object> supplier;

    SpringCloudFunctionLoader(@Autowired FunctionCatalog catalog, @Autowired FunctionInspector inspector) {
        this.catalog = catalog;
        this.inspector = inspector;
    }

    void loadFunction() {
        this.function = this.catalog.lookupFunction("function");
    }

    SpringCloudMethod getFunction() {
        return new SpringCloudFunction(function, inspector);
    }
}
