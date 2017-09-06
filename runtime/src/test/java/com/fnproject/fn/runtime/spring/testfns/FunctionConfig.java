package com.fnproject.fn.runtime.spring.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.spring.SpringCloudFunctionInvoker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.function.context.FunctionScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Import(ContextFunctionCatalogAutoConfiguration.class)
@ComponentScan(basePackages = "com.fnproject.fn")
public class FunctionConfig {
    public static String consumedValue;
    public static String suppliedValue = "Hello";

    @FnConfiguration
    public static void configure(RuntimeContext ctx) {
        ctx.setInvoker(new SpringCloudFunctionInvoker(ctx));
    }

    @Bean
    public Supplier<String> supplier() {
        return () -> suppliedValue;
    }

    @Bean
    public Consumer<String> consumer() {
        return (str) -> consumedValue = str;
    }

    @Bean
    public Function<String, String> function() {
        return String::toLowerCase;
    }
    @Bean
    public Function<String, String> upperCaseFunction() {
        return String::toUpperCase;
    }

    @Bean
    public String notAFunction() { return "NotAFunction"; }

    // Empty entrypoint that isn't used but necessary for the EntryPoint. Our invoker ignores this and loads our own
    // function to invoke
    public void handleRequest() { }

    public static class Name {
        public final String first;
        public final String last;

        public Name(String first, String last) {
            this.first = first;
            this.last = last;
        }
    }
}
