package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.MethodFunctionInvoker;
import com.fnproject.fn.runtime.spring.function.SpringCloudMethod;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Flux;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SpringCloudFunctionInvoker extends MethodFunctionInvoker implements Closeable {
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

    protected Object tryInvoke(SpringCloudMethod method, Object[] userFunctionParams) {
        return convertFromFlux(method.invoke(convertToFlux(userFunctionParams)));
    }

    private Object convertFromFlux(Flux<?> output) {
        List<Object> result = new ArrayList<>();
        for (Object val : output.toIterable()) {
            result.add(val);
        }
        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return result;
        }
    }

    private Object convertToFlux(Object[] params) {
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
