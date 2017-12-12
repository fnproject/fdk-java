package com.fnproject.springframework.function;

import com.fnproject.springframework.function.exception.SpringCloudFunctionNotFoundException;
import com.fnproject.springframework.function.functions.SpringCloudConsumer;
import com.fnproject.springframework.function.functions.SpringCloudFunction;
import com.fnproject.springframework.function.functions.SpringCloudMethod;
import com.fnproject.springframework.function.functions.SpringCloudSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionInspector;
import org.springframework.cloud.function.core.FunctionCatalog;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Loader for Spring Cloud Functions
 * <p>
 * Looks up Functions from the {@link FunctionCatalog} (which is likely populated by your
 * function class, see {@link SpringCloudFunctionInvoker#SpringCloudFunctionInvoker(Class<?>)})
 * <p>
 * Lookup is in the following order:
 * <p>
 * Environment variable `FN_SPRING_FUNCTION` returning a `Function`
 * Environment variable `FN_SPRING_CONSUMER` returning a `Consumer`
 * Environment variable `FN_SPRING_SUPPLIER` returning a `Supplier`
 * Bean named `function` returning a `Function`
 * Bean named `consumer` returning a `Consumer`
 * Bean named `supplier` returning a `Supplier`
 */
public class SpringCloudFunctionLoader {
    public static final String DEFAULT_SUPPLIER_BEAN = "supplier";
    public static final String DEFAULT_CONSUMER_BEAN = "consumer";
    public static final String DEFAULT_FUNCTION_BEAN = "function";

    public static final String ENV_VAR_FUNCTION_NAME = "FN_SPRING_FUNCTION";
    public static final String ENV_VAR_CONSUMER_NAME = "FN_SPRING_CONSUMER";
    public static final String ENV_VAR_SUPPLIER_NAME = "FN_SPRING_SUPPLIER";

    private final FunctionCatalog catalog;
    private final FunctionInspector inspector;

    private Function<Flux<?>, Flux<?>> function;
    private Consumer<Flux<?>> consumer;
    private Supplier<Flux<?>> supplier;

    SpringCloudFunctionLoader(@Autowired FunctionCatalog catalog, @Autowired FunctionInspector inspector) {
        this.catalog = catalog;
        this.inspector = inspector;
    }

    void loadFunction() {

        loadSpringCloudFunctionFromEnvVars();

        if (noSpringCloudFunctionFound()) {
            loadSpringCloudFunctionFromDefaults();
        }

        if (noSpringCloudFunctionFound()) {
            throw new SpringCloudFunctionNotFoundException("No Spring Cloud Function found.");
        }
    }


    private void loadSpringCloudFunctionFromEnvVars() {
        String functionName = System.getenv(ENV_VAR_FUNCTION_NAME);
        if (functionName != null) {
            function = this.catalog.lookupFunction(functionName);
        }

        String consumerName = System.getenv(ENV_VAR_CONSUMER_NAME);
        if (consumerName != null) {
            consumer = this.catalog.lookupConsumer(consumerName);
        }

        String supplierName = System.getenv(ENV_VAR_SUPPLIER_NAME);
        if (supplierName != null) {
            supplier = this.catalog.lookupSupplier(supplierName);
        }
    }

    private void loadSpringCloudFunctionFromDefaults() {
        function = this.catalog.lookupFunction(DEFAULT_FUNCTION_BEAN);
        consumer = this.catalog.lookupConsumer(DEFAULT_CONSUMER_BEAN);
        supplier = this.catalog.lookupSupplier(DEFAULT_SUPPLIER_BEAN);
    }


    private boolean noSpringCloudFunctionFound() {
        return function == null && consumer == null && supplier == null;
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
