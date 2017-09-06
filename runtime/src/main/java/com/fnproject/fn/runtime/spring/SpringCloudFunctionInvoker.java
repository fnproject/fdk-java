package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.MethodFunctionInvoker;
import com.fnproject.fn.runtime.spring.function.SpringCloudFunction;
import com.fnproject.fn.runtime.spring.function.SpringCloudMethod;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringCloudFunctionInvoker extends MethodFunctionInvoker {
    private final SpringCloudFunctionLoader loader;

    SpringCloudFunctionInvoker(SpringCloudFunctionLoader loader) {
        this.loader = loader;
    }

    public SpringCloudFunctionInvoker(Class<?> configClass) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(configClass);
        ConfigurableApplicationContext applicationContext = builder.web(false).run();
        loader = applicationContext.getAutowireCapableBeanFactory().createBean(SpringCloudFunctionLoader.class);
        loader.loadFunction();
    }

    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        SpringCloudMethod method = loader.getFunction();

        Object[] userFunctionParams = coerceParameters(ctx, method, evt);

        Object rawResult;
        rawResult = convertFromFlux(method.invoke(convertToFlux(userFunctionParams[0])));
        return coerceReturnValue(ctx, method, ((List) rawResult).get(0));
    }

    private Object convertFromFlux(Flux<?> output) {
        List<Object> result = new ArrayList<>();
        for (Object val : output.toIterable()) {
            result.add(val);
        }
        return result;
    }

    private Object convertToFlux(Object userFunctionParam) {
        return Flux.just(userFunctionParam);
    }
}
