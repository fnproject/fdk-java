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
public class FunctionConfig {
    @FnConfiguration
    public static void configure(RuntimeContext ctx) {
        ctx.setInvoker(new SpringCloudFunctionInvoker(FunctionConfig.class));
    }
    public void handleRequest() { }

    @Bean
    public Supplier<String> supplier() {
        return () -> "Hello";
    }

    @Bean
    public Consumer<String> consumer() {
        return System.out::println;
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

    public static class Name {
        public final String first;
        public final String last;

        public Name(String first, String last) {
            this.first = first;
            this.last = last;
        }
    }
}
