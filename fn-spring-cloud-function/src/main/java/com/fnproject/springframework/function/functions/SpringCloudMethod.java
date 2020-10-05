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

package com.fnproject.springframework.function.functions;

import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.TypeWrapper;
import com.fnproject.springframework.function.SimpleTypeWrapper;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.Arrays;


/**
 * Superclass for classes which represent java.util.function.* objects as
 * Spring Cloud Functions
 */
public abstract class SpringCloudMethod implements MethodWrapper {
    private FunctionInspector inspector;

    SpringCloudMethod(FunctionInspector inspector) {
        this.inspector = inspector;
    }

    @Override
    public Class<?> getTargetClass() {
        return getFunction().getClass();
    }

    @Override
    public Method getTargetMethod() {
        Class<?> cls = getTargetClass();
        String methodName = getMethodName();

        return Arrays.stream(cls.getMethods())
                .filter((m) -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find " + methodName + " on " + cls));
    }

    /**
     * Returns the name of the method used to invoke the function object.
     * e.g. {@code apply} for {@link java.util.function.Function},
     * {@code accept} for {@link java.util.function.Consumer},
     * {@code get} for {@link java.util.function.Supplier}
     *
     * @return name of the method used to invoke the function object
     */
    protected abstract String getMethodName();

    /**
     * Returns the target function object as an {@link Object}.
     * (used for type inspection purposes)
     *
     * @return target function object
     */
    protected abstract Object getFunction();

    @Override
    public TypeWrapper getParamType(int index) {
        return new SimpleTypeWrapper(inspector.getInputType(getFunction()));
    }

    @Override
    public TypeWrapper getReturnType() {
        return new SimpleTypeWrapper(inspector.getOutputType(getFunction()));
    }

    /**
     * Invoke the target function object
     *
     * @param arg
     * @return
     */
    public abstract Flux<?> invoke(Flux<?> arg);
}
