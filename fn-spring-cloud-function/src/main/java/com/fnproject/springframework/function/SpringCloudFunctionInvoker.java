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

import com.fnproject.fn.api.*;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.springframework.function.functions.SpringCloudMethod;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Flux;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A {@link FunctionInvoker} for Spring Cloud Functions
 *
 * Call {@link RuntimeContext#setInvoker(FunctionInvoker)} before any function invocations
 * to use this.
 */
public class SpringCloudFunctionInvoker implements FunctionInvoker, Closeable {
    private final SpringCloudFunctionLoader loader;
    private ConfigurableApplicationContext applicationContext;

    SpringCloudFunctionInvoker(SpringCloudFunctionLoader loader) {
        this.loader = loader;
    }

    /**
     * A common pattern is to call this function from within a Configuration method of
     * a {@link org.springframework.beans.factory.annotation.Configurable} function.
     *
     * @param configClass The class which defines your Spring Cloud Function @Beans
     */
    public SpringCloudFunctionInvoker(Class<?> configClass) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(configClass);
        applicationContext = builder.web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).run();
        loader = applicationContext.getAutowireCapableBeanFactory().createBean(SpringCloudFunctionLoader.class);
        loader.loadFunction();
    }

    /**
     * Invoke the user's function with params generated from:
     *
     * @param ctx the {@link InvocationContext}
     * @param evt the {@link InputEvent}
     */
    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        SpringCloudMethod method = loader.getFunction();

        Object[] userFunctionParams = coerceParameters(ctx, method, evt);
        Object result = tryInvoke(method, userFunctionParams);
        return coerceReturnValue(ctx, method, result);
    }

    private Object[] coerceParameters(InvocationContext ctx, MethodWrapper targetMethod, InputEvent evt) {
        try {
            Object[] userFunctionParams = new Object[targetMethod.getParameterCount()];

            for (int paramIndex = 0; paramIndex < userFunctionParams.length; paramIndex++) {
                userFunctionParams[paramIndex] = coerceParameter(ctx, targetMethod, paramIndex, evt);
            }

            return userFunctionParams;

        } catch (RuntimeException e) {
            throw new FunctionInputHandlingException("An exception was thrown during Input Coercion: " + e.getMessage(), e);
        }
    }

    private Optional<OutputEvent> coerceReturnValue(InvocationContext ctx, MethodWrapper method, Object rawResult) {
        try {
            return Optional.of((ctx.getRuntimeContext()).getOutputCoercions(method.getTargetMethod())
                    .stream()
                    .map((c) -> c.wrapFunctionResult(ctx, method, rawResult))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElseThrow(() -> new FunctionOutputHandlingException("No coercion found for return type")));

        } catch (RuntimeException e) {
            throw new FunctionOutputHandlingException("An exception was thrown during Output Coercion: " + e.getMessage(), e);
        }
    }

    private Object coerceParameter(InvocationContext ctx, MethodWrapper targetMethod, int param, InputEvent evt) {
        RuntimeContext runtimeContext = ctx.getRuntimeContext();

        return runtimeContext.getInputCoercions(targetMethod, param)
                .stream()
                .map((c) -> c.tryCoerceParam(ctx, param, evt, targetMethod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new FunctionInputHandlingException("No type coercion for argument " + param + " of " + targetMethod + " of found"));
    }

    // NB: this could be private except it's tested directly
    protected Object tryInvoke(SpringCloudMethod method, Object[] userFunctionParams) {
        Object userFunctionParam = null;
        if (userFunctionParams.length > 0) {
            userFunctionParam = userFunctionParams[0];
        }
        Flux<?> input = convertToFlux(userFunctionParams);
        Flux<?> result = method.invoke(input);
        return convertFromFlux(userFunctionParam, result);
    }

    private Object convertFromFlux(Object preFluxifiedInput, Flux<?> output) {
        List<Object> result = new ArrayList<>();
        for (Object val : output.toIterable()) {
            result.add(val);
        }
        if (result.isEmpty()) {
            return null;
        } else if (isSingleValue(preFluxifiedInput) && result.size() == 1) {
            return result.get(0);
        } else {
            return result;
        }
    }

    private boolean isSingleValue(Object input) {
        return !(input instanceof Collection);
    }

    private Flux<?> convertToFlux(Object[] params) {
        if (params.length == 0) {
            return Flux.empty();
        }
        Object firstParam = params[0];
        if (firstParam instanceof Collection){
            return Flux.fromIterable((Collection) firstParam);
        }
        return Flux.just(firstParam);
    }

    @Override
    public void close() {
        applicationContext.close();
    }
}
