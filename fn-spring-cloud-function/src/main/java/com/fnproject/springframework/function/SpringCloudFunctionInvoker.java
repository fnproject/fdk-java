package com.fnproject.springframework.function;

import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.springframework.function.functions.SpringCloudMethod;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Flux;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SpringCloudFunctionInvoker implements FunctionInvoker, Closeable {
    private final SpringCloudFunctionLoader loader;
    private ConfigurableApplicationContext applicationContext;

    SpringCloudFunctionInvoker(SpringCloudFunctionLoader loader) {
        this.loader = loader;
    }

    public SpringCloudFunctionInvoker(Class<?> configClass) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(configClass);
        applicationContext = builder.web(false).run();
        loader = applicationContext.getAutowireCapableBeanFactory().createBean(SpringCloudFunctionLoader.class);
        loader.loadFunction();
    }

    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        SpringCloudMethod method = loader.getFunction();

        Object[] userFunctionParams = coerceParameters(ctx, method, evt);
        Object result = tryInvoke(method, userFunctionParams);
        return coerceReturnValue(ctx, method, result);
    }

    // TODO (MJG): this is copy-pasted from the 'runtime' module
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

    // TODO (MJG): this is copy-pasted from the 'runtime' module
    protected Optional<OutputEvent> coerceReturnValue(InvocationContext ctx, MethodWrapper method, Object rawResult) {
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

    // TODO (MJG): this is copy-pasted from the 'runtime' module
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

    // TODO: this could be private except it's tested directly.
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
    public void close() throws IOException {
        applicationContext.close();
    }
}
