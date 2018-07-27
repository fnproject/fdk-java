package com.fnproject.springframework.function.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.springframework.function.SpringCloudFunctionInvoker;
import org.springframework.cloud.function.context.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ContextFunctionCatalogAutoConfiguration.class)
public class EmptyFunctionConfig {
    @FnConfiguration
    public static void configure(RuntimeContext ctx) {
        ctx.setInvoker(new SpringCloudFunctionInvoker(EmptyFunctionConfig.class));
    }
    public void handleRequest() { }

}
