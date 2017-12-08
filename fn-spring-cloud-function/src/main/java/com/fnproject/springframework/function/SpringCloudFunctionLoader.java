package com.fnproject.springframework.function;

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
 *
 * Looks up Functions from the {@link FunctionCatalog} (which is likely populated by your
 * function class, see {@link SpringCloudFunctionInvoker#SpringCloudFunctionInvoker(Class<?>)})
 *
 * Lookup is in the following order:
 *
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
    private String functionName;
    private String supplierName;
    private String consumerName;

    SpringCloudFunctionLoader(@Autowired FunctionCatalog catalog, @Autowired FunctionInspector inspector) {
        this.catalog = catalog;
        this.inspector = inspector;
    }

    void loadFunction() {
        boolean foundName = checkForBeanNameInEnvVars();
        if (foundName) {
            if (functionName != null) {
                function = this.catalog.lookupFunction(functionName);
            } else if (consumerName != null) {
                consumer = this.catalog.lookupConsumer(consumerName);
            } else if (supplierName != null) {
                supplier = this.catalog.lookupSupplier(supplierName);
            }
            // TODO: throw exception if we don't find the specified function?
            // TODO: throw exception if multiple values are set?
        } else {
            function = this.catalog.lookupFunction(DEFAULT_FUNCTION_BEAN);
            if (function == null) {
                consumer = this.catalog.lookupConsumer(DEFAULT_CONSUMER_BEAN);
                if (consumer == null) {
                    supplier = this.catalog.lookupSupplier(DEFAULT_SUPPLIER_BEAN);
                }
            }
        }
        // TODO throw exception if no function found?
    }

    private boolean checkForBeanNameInEnvVars() {
        String functionName = System.getenv(ENV_VAR_FUNCTION_NAME);
        if (functionName != null) {
            this.functionName = functionName;
            return true;
        }
        String consumerName = System.getenv(ENV_VAR_CONSUMER_NAME);
        if (consumerName != null) {
            this.consumerName = consumerName;
            return true;
        }
        String supplierName = System.getenv(ENV_VAR_SUPPLIER_NAME);
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
