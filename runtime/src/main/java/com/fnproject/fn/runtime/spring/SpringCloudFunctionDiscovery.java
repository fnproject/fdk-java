package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.exception.InvalidEntryPointException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringCloudFunctionDiscovery {
    public static final String PROPERTY_KEY_FUNCTION_NAME = "function.name";
    public static final String PROPERTY_KEY_SUPPLIER_NAME = "supplier.name";
    public static final String PROPERTY_KEY_CONSUMER_NAME = "consumer.name";
    private RuntimeContext runtimeContext;

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    private Supplier<?> supplier;
    private Consumer<?> consumer;
    private Function<?, ?> function;

    public SpringCloudFunctionDiscovery(RuntimeContext runtimeContext, ApplicationContext context) {
        this.runtimeContext = runtimeContext;
        this.applicationContext = context;
    }

    public SpringCloudFunctionDiscovery(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public void discover() {
        Object fn;
        try {
            fn = runtimeContext.getTargetMethod().invoke(runtimeContext.getInvokeInstance().orElse(null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidEntryPointException("Could not invoke " + runtimeContext.getMethod().getLongName() + " which is unsupported", e);
        }
        if (!assignFnToField(fn)) {
            throw new InvalidEntryPointException("The function loaded from " + runtimeContext.getMethod().getLongName() +
                    " is not of type Supplier, Consumer or Function, but is " + fn.getClass() + " which is unsupported");
        };
        tryLoadingFromContext();
    }

    private void tryLoadingFromContext() {
        if (this.applicationContext != null) {
            tryLoadingSupplierFromContext();
            tryLoadingConsumerFromContext();
            tryLoadingFunctionFromContext();
        }
    }

    private void tryLoadingConsumerFromContext() {
        String consumerName = this.applicationContext.getEnvironment().getProperty(PROPERTY_KEY_CONSUMER_NAME);
        if (consumerName != null) {
            try {
                assignFnToField((Consumer<?>) this.applicationContext.getBean(consumerName, Consumer.class));
            } catch(BeansException e) {
                throw new InvalidEntryPointException("Could not load desired function", e);
            }
        }
    }

    private void tryLoadingSupplierFromContext() {
        String supplierName = this.applicationContext.getEnvironment().getProperty(PROPERTY_KEY_SUPPLIER_NAME);
        if (supplierName != null) {
            try {
                assignFnToField((Supplier<?>) this.applicationContext.getBean(supplierName, Supplier.class));
            } catch(BeansException e) {
                throw new InvalidEntryPointException("Could not load desired function", e);
            }
        }
    }

    private void tryLoadingFunctionFromContext() {
        String fnName = this.applicationContext.getEnvironment().getProperty(PROPERTY_KEY_FUNCTION_NAME);
        if (fnName != null) {
            try {
                assignFnToField((Function<?, ?>) this.applicationContext.getBean(fnName, Function.class));
            } catch (BeansException e) {
                throw new InvalidEntryPointException("Could not load desired function", e);
            }
        }
    }

    private boolean assignFnToField(Object fn) {
        if (fn instanceof Function) {
            this.function = (Function<?, ?>) fn;
            this.consumer = null;
            this.supplier = null;
        } else if (fn instanceof Consumer) {
            this.function = null;
            this.consumer = (Consumer<?>) fn;
            this.supplier = null;
        } else if (fn instanceof Supplier) {
            this.function = null;
            this.consumer = null;
            this.supplier = (Supplier<?>) fn;
        } else {
            return false;
        }
        return true;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public Function getFunction() {
        return function;
    }

}
