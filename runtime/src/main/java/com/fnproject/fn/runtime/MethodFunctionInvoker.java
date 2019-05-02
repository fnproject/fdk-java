package com.fnproject.fn.runtime;


import com.fnproject.fn.api.*;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * Method function invoker
 * <p>
 * <p>
 * This handles the binding and invocation of function calls via java methods.
 */
public class MethodFunctionInvoker implements FunctionInvoker {

    /*
    * If enabled, print the logging framing content
    */
    public void logFramer(FunctionRuntimeContext rctx, InputEvent evt) {
        String framer = rctx.getConfigurationByKey("FN_LOGFRAME_NAME").orElse("");

        if (framer != "") {
            String valueSrc = rctx.getConfigurationByKey("FN_LOGFRAME_HDR").orElse("");

            if (valueSrc != "") {
                String id = evt.getHeaders().get(valueSrc).orElse("");
                if (id != "") {
                    System.out.println("\n" + framer + "=" + id + "\n");
                    System.err.println("\n" + framer + "=" + id + "\n");
                }
            }
        }
    }


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

        logFramer(runtimeContext, evt);

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
