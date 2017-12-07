package com.fnproject.springframework.function.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.springframework.function.SpringCloudFunctionInvoker;
import org.springframework.cloud.function.context.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Import(ContextFunctionCatalogAutoConfiguration.class)
public class EmptyFunctionConfig {
    @FnConfiguration
    public static void configure(RuntimeContext ctx) {
        ctx.setInvoker(new SpringCloudFunctionInvoker(EmptyFunctionConfig.class));
    }
    public void handleRequest() { }

}
