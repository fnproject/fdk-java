package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.runtime.spring.function.SpringCloudConsumer;
import com.fnproject.fn.runtime.spring.function.SpringCloudFunction;
import com.fnproject.fn.runtime.spring.function.SpringCloudMethod;
import com.fnproject.fn.runtime.spring.function.SpringCloudSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.cloud.function.registry.FunctionCatalog;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringCloudFunctionLoader {
    public static final String DEFAULT_SUPPLIER_BEAN = "supplier";
    public static final String DEFAULT_CONSUMER_BEAN = "consumer";
    public static final String DEFAULT_FUNCTION_BEAN = "function";

    public static final String SYSTEM_PROP_KEY_SUPPLIER_NAME = "supplier.name";
    private static final String SYSTEM_PROP_KEY_CONSUMER_NAME = "consumer.name";
    private static final String SYSTEM_PROP_KEY_FUNCTION_NAME = "function.name";

    private final FunctionCatalog catalog;
    private final FunctionInspector inspector;

    private Function<Object, Object> function;
    private Consumer<Object> consumer;
    private Supplier<Object> supplier;
    private String functionName;
    private String supplierName;
    private String consumerName;

    SpringCloudFunctionLoader(@Autowired FunctionCatalog catalog, @Autowired FunctionInspector inspector) {
        this.catalog = catalog;
        this.inspector = inspector;
    }

    void loadFunction() {
        boolean foundName = checkForBeanNameInSystemProps();
        // TODO: Check environment variables for SPRING_CLOUD_FUNCTION/SPRING_CLOUD_CONSUMER/SPRING_CLOUD_SUPPLIER and
        // use them in preference to system props
        if (foundName) {
            if (functionName != null) {
                function = this.catalog.lookupFunction(functionName);
            } else if (consumerName != null) {
                consumer = this.catalog.lookupConsumer(consumerName);
            } else if (supplierName != null) {
                supplier = this.catalog.lookupSupplier(supplierName);
            }
            // TODO: throw exception if we don't find the specified function
            // TODO: throw exception if multiple values are set
        } else {
            function = this.catalog.lookupFunction(DEFAULT_FUNCTION_BEAN);
            if (function == null) {
                consumer = this.catalog.lookupConsumer(DEFAULT_CONSUMER_BEAN);
                if (consumer == null) {
                    supplier = this.catalog.lookupSupplier(DEFAULT_SUPPLIER_BEAN);
                }
            }
        }
        // TODO throw exception if no function found
    }

    private boolean checkForBeanNameInSystemProps() {
        String functionName = System.getProperty(SYSTEM_PROP_KEY_FUNCTION_NAME);
        if (functionName != null) {
            this.functionName = functionName;
            return true;
        }
        String consumerName = System.getProperty(SYSTEM_PROP_KEY_CONSUMER_NAME);
        if (consumerName != null) {
            this.consumerName = consumerName;
            return true;
        }
        String supplierName = System.getProperty(SYSTEM_PROP_KEY_SUPPLIER_NAME);
        if (supplierName != null) {
            this.supplierName = supplierName;
            return true;
        }
        return false;
    }

    SpringCloudMethod getFunction() {
        if (function != null) {
            return new SpringCloudFunction(function, inspector);
        } else if (consumer != null) {
            return new SpringCloudConsumer(consumer, inspector);
        } else {
            return new SpringCloudSupplier(supplier, inspector);
        }
    }
}
