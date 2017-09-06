package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.runtime.DefaultTypeWrapper;
import com.fnproject.fn.runtime.exception.InvalidEntryPointException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.cloud.function.registry.FunctionCatalog;
import org.springframework.cloud.function.support.FluxFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class SpringCloudFunction implements MethodWrapper, ApplicationContextAware {
    public static final String PROPERTY_KEY_FUNCTION_NAME = "function.name";
    public static final String PROPERTY_KEY_SUPPLIER_NAME = "supplier.name";
    public static final String PROPERTY_KEY_CONSUMER_NAME = "consumer.name";

    @Autowired
    FunctionInspector inspector;

    @Autowired
    private FunctionCatalog catalog;

    private ApplicationContext applicationContext;


    @Value("${function.name:function}")
    String functionName;

    @Value("${consumer.name:consumer")
    String consumerName;

    @Value("${supplier.name:supplier}")
    String supplierName;

    private Supplier supplier;
    private Consumer consumer;
    private Function function;

    public SpringCloudFunction(ApplicationContext context) {
        this.applicationContext = context;
    }

    public void discover() {
        this.function = catalog.lookupFunction("function");
    }

    public <T> Supplier<T> getSupplier() {
        return (Supplier<T>) supplier;
    }

    public <T> Consumer<T> getConsumer() {
        return (Consumer<T>) consumer;
    }

    public <I, O> Function<I, O> getFunction() {
        return (Function<I, O>) function;
    }

    @Override
    public Class<?> getTargetClass() {
        if (function != null) {
            return function.getClass();
        } else if (consumer != null) {
            return consumer.getClass();
        } else if (supplier != null) {
            return supplier.getClass();
        } else {
            throw new IllegalStateException("No discovered function inside " + getClass());
        }
    }

    @Override
    public Method getTargetMethod() {
        Class<?> cls;
        String methodName;
        if (this.function != null) {
            cls = this.function.getClass();
            methodName = "apply";
        } else if (this.consumer != null) {
            cls = this.consumer.getClass();
            methodName = "accept";
        } else if (this.supplier != null) {
            cls = this.supplier.getClass();
            methodName = "get";
        } else {
            throw new IllegalStateException("No discovered function inside " + getClass());
        }
        return Arrays.stream(cls.getMethods())
                .filter((m) -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find " + methodName + " on " + cls));
    }

    @Override
    public TypeWrapper getParamType(int index) {
        if (this.function != null) {
            return new DefaultTypeWrapper(inspector.getInputType(this.function));
        } else if (this.consumer != null) {
            return new DefaultTypeWrapper(inspector.getInputType(this.consumer));
        } else if (this.supplier != null) {
            return new DefaultTypeWrapper(inspector.getInputType(this.supplier));
        } else {
            throw new IllegalStateException("No discovered function inside " + getClass());
        }
    }

    @Override
    public TypeWrapper getReturnType() {
        if (this.function != null) {
            return new DefaultTypeWrapper(inspector.getOutputType(this.function));
        } else if (this.consumer != null) {
            return new DefaultTypeWrapper(inspector.getOutputType(this.consumer));
        } else if (this.supplier != null) {
            return new DefaultTypeWrapper(inspector.getOutputType(this.supplier));
        } else {
            throw new IllegalStateException("No discovered function inside " + getClass());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }

    public Flux<?> invoke(Object... userFunctionParams) {
        if (function != null) {
            return (Flux<?>) function.apply(userFunctionParams[0]);
        }
        return null;
    }
}
