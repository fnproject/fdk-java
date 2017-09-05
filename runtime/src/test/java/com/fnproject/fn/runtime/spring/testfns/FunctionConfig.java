package com.fnproject.fn.runtime.spring.testfns;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionConfig {
    @Autowired
    private Name name;

    public static String consumedValue;
    public static String suppliedValue = "Hello";

    @Bean
    public Supplier<String> supplier() {
        return () -> suppliedValue;
    }

    @Bean
    public Consumer<String> consumer() {
        return (str) -> consumedValue = str;
    }

    @Bean
    public Function<String, String> upperCaseFunction() {
        return String::toUpperCase;
    }

    @Bean
    public String notAFunction() { return "NotAFunction"; }

    @Bean
    private Name defaultName()  {
        return new Name("John", "Doe");
    }

    public static class Name {
        public final String first;
        public final String last;

        public Name(String first, String last) {
            this.first = first;
            this.last = last;
        }
    }
}
