package com.fnproject.fn.runtime;


import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

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
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {

        Object[] userFunctionParams = doInputCoercions(ctx, evt);

        FunctionRuntimeContext runtimeContext = (FunctionRuntimeContext) ctx.getRuntimeContext();
        Method targetMethod = runtimeContext.getTargetMethod();
        Object rawResult;

        try {
            rawResult = targetMethod.invoke(ctx.getRuntimeContext().getInvokeInstance().orElse(null), userFunctionParams);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InternalFunctionInvocationException(e.getCause().getMessage(), e.getCause());
        }

        return doOutputCoercion(ctx, runtimeContext, targetMethod, rawResult);

    }

    private Object[] doInputCoercions(InvocationContext ctx, InputEvent evt) {

        try {

            FunctionRuntimeContext runtimeContext = (FunctionRuntimeContext) ctx.getRuntimeContext();
            Method targetMethod = runtimeContext.getTargetMethod();
            Class<?>[] paramTypes = targetMethod.getParameterTypes();

            Object[] userFunctionParams = new Object[paramTypes.length];

            for (int i = 0; i < userFunctionParams.length; i++) {
                int param = i;
                Optional<?> arg = Optional.empty();
                for (InputCoercion<?> ic : runtimeContext.getInputCoercions(targetMethod, param)) {
                    arg = ic.tryCoerceParam(ctx, param, evt);
                    if (arg.isPresent()) {
                        break;
                    }
                }

                userFunctionParams[i] = arg.orElseThrow(() -> new InputCoercion.InvalidFunctionInputException(
                        "No type coercion was able to convert the input provided to the function into a usable form. (param=" +
                                param + ", method=" + targetMethod + ")"));
            }

            return userFunctionParams;
        } catch (InputCoercion.InvalidFunctionInputException e) {
            // just rethrow
            throw e;
        } catch (RuntimeException e) {
            // Any other exception, we wrap into our own
            throw new FunctionInputHandlingException("An exception was thrown during Input Coercion: " + e.getMessage(), e);
        }

    }


    private Optional<OutputEvent> doOutputCoercion(InvocationContext ctx, FunctionRuntimeContext runtimeContext, Method targetMethod, Object rawResult) {

        try {
            return Optional.of(runtimeContext.getOutputCoercions(targetMethod)
                    .stream()
                    .map((c) -> c.wrapFunctionResult(ctx, rawResult))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElseThrow(() -> new FunctionOutputHandlingException("No coercion found for return type")));

        } catch (RuntimeException e) {
            throw new FunctionOutputHandlingException("An exception was thrown during Output Coercion: " + e.getMessage(), e);
        }
    }
}
