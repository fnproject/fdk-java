package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.MethodFunctionInvoker;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringCloudFunctionInvoker extends MethodFunctionInvoker {
    private final RuntimeContext runtimeContext;

    private SpringCloudFunction method;

    private final ApplicationContext applicationContext;

    public SpringCloudFunctionInvoker(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;

        SpringApplicationBuilder builder = new SpringApplicationBuilder(runtimeContext.getMethod().getTargetClass());
        applicationContext = builder.web(false).run();

        method = new SpringCloudFunction(applicationContext);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(method);
    }

    @Bean
    RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        method.discover();

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
