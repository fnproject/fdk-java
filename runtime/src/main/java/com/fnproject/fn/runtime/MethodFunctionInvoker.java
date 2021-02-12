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

package com.fnproject.fn.runtime;


import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;

/**
 * Method function invoker
 * <p>
 * <p>
 * This handles the binding and invocation of function calls via java methods.
 */
public class MethodFunctionInvoker implements FunctionInvoker {


    /**
     * Invoke the function wrapped by this loader
     *
     * @param evt The function event
     * @return the function response
     * @throws InternalFunctionInvocationException if the invocation fails
     */
    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) throws InternalFunctionInvocationException {
        FunctionRuntimeContext runtimeContext = (FunctionRuntimeContext) ctx.getRuntimeContext();
        MethodWrapper method = runtimeContext.getMethodWrapper();

        Object[] userFunctionParams = coerceParameters(ctx, method, evt);

        Object rawResult;


        try {
            rawResult = method.getTargetMethod().invoke(ctx.getRuntimeContext().getInvokeInstance().orElse(null), userFunctionParams);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InternalFunctionInvocationException(e.getCause().getMessage(), e.getCause());
        }

        return coerceReturnValue(ctx, method, rawResult);
    }

    protected Object[] coerceParameters(InvocationContext ctx, MethodWrapper targetMethod, InputEvent evt) {
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

    protected Optional<OutputEvent> coerceReturnValue(InvocationContext ctx, MethodWrapper method, Object rawResult) {
        try {
            return Optional.of(ctx.getRuntimeContext().getOutputCoercions(method.getTargetMethod())
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

}
