/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.springframework.function.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.springframework.function.SpringCloudFunctionFeature;
import com.fnproject.springframework.function.SpringCloudFunctionInvoker;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Import(ContextFunctionCatalogAutoConfiguration.class)
@FnFeature(SpringCloudFunctionFeature.class)
public class FunctionConfig {

    public void handleRequest() {
    }

    @Bean
    public Supplier<String> supplier() {
        return () -> "Hello";
    }

    @Bean
    public Consumer<String> consumer() {
        System.out.println("LOADED");
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
    public String notAFunction() {
        return "NotAFunction";
    }

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
