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

package com.fnproject.springframework.function;

import net.jodah.typetools.TypeResolver;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.FunctionInspector;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleFunctionInspector implements FunctionInspector {
    @Override
    public FunctionRegistration<?> getRegistration(Object function) {
        return null;
    }

    @Override
    public boolean isMessage(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Class<?> getInputType(Object function) {
        if (function instanceof Function) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            return types[0];
        } else if (function instanceof Consumer) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Consumer.class, function.getClass());
            return types[0];
        } else if (function instanceof Supplier) {
            return Void.class;
        } else {
            throw new IllegalStateException("You cannot get the input type of a function that doesn't implement one of the java.util.function interfaces");
        }
    }

    @Override
    public Class<?> getOutputType(Object function) {
        if (function instanceof Function) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            return types[0];
        } else if (function instanceof Consumer) {
            return Void.class;
        } else if (function instanceof Supplier) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Consumer.class, function.getClass());
            return types[0];
        } else {
            throw new IllegalStateException("You cannot get the output type of a function that doesn't implement one of the java.util.function interfaces");
        }
    }

    @Override
    public Class<?> getInputWrapper(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Class<?> getOutputWrapper(Object function) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getName(Object function) {
        return function.toString();
    }
}
