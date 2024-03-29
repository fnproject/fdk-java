/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.springframework.function;

import com.fnproject.springframework.function.exception.SpringCloudFunctionNotFoundException;
import com.fnproject.springframework.function.functions.SpringCloudConsumer;
import com.fnproject.springframework.function.functions.SpringCloudFunction;
import com.fnproject.springframework.function.functions.SpringCloudMethod;
import com.fnproject.springframework.function.functions.SpringCloudSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Loader for Spring Cloud Functions
 * <p>
 * Looks up Functions from the {@link FunctionCatalog} (which is likely populated by your
 * function class, see {@link SpringCloudFunctionInvoker#SpringCloudFunctionInvoker(SpringCloudFunctionLoader)})
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

    private final SimpleFunctionRegistry registry;

    private Function<Flux<?>, Flux<?>> function;
    private Consumer<Flux<?>> consumer;
    private Supplier<Flux<?>> supplier;

    SpringCloudFunctionLoader(@Autowired SimpleFunctionRegistry registry) {
        this.registry = registry;
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
            function = this.registry.lookup(Function.class, functionName);
        }

        String consumerName = System.getenv(ENV_VAR_CONSUMER_NAME);
        if (consumerName != null) {
            consumer = this.registry.lookup(Consumer.class, consumerName);
        }

        String supplierName = System.getenv(ENV_VAR_SUPPLIER_NAME);
        if (supplierName != null) {
            supplier = this.registry.lookup(Supplier.class, supplierName);
        }
    }

    private void loadSpringCloudFunctionFromDefaults() {
        function = this.registry.lookup(Function.class, DEFAULT_FUNCTION_BEAN);
        consumer = this.registry.lookup(Consumer.class, DEFAULT_CONSUMER_BEAN);
        supplier = this.registry.lookup(Supplier.class, DEFAULT_SUPPLIER_BEAN);
    }


    private boolean noSpringCloudFunctionFound() {
        return function == null && consumer == null && supplier == null;
    }


    SpringCloudMethod getFunction() {
        if (function != null) {
            return new SpringCloudFunction(function, registry);
        } else if (consumer != null) {
            return new SpringCloudConsumer(consumer, registry);
        } else {
            return new SpringCloudSupplier(supplier, registry);
        }
    }
}
