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

package com.fnproject.fn.api;

import java.lang.reflect.Method;

/**
 * Represents a method, similar to {@link Method} but with methods for resolving
 * parameter and return types that are reified generics.
 */
public interface MethodWrapper {
    /**
     * Get the target class for the function invocation
     *
     * The class from which the wrapper was created, may not necessarily be the declaring class of the method, but a
     * subclass.
     *
     * @return the class the user has configured as the function entrypoint
     */
    Class<?> getTargetClass();

    /**
     * Gets the underlying wrapped {@link Method}
     *
     * @return {@link Method} which this class wraps
     */
    Method getTargetMethod();

    /**
     * Get the {@link TypeWrapper} for a parameter specified by {@code index}
     * @param index    index of the parameter whose type to retrieve
     * @return the type of the parameter at {@code index}, reified generics will resolve to the reified type
     *         and not {@link Object}
     */
    TypeWrapper getParamType(int index);

    /**
     * Get the {@link TypeWrapper} for the return type of the method.
     * @return the return type, reified generics will resolve to the reified type and not {@link Object}
     */
    TypeWrapper getReturnType();

    /**
     * Get the name of the method including the package path built from {@link Class#getCanonicalName()} and
     * {@link Method#getName()}. e.g. {@code com.fnproject.fn.api.MethodWrapper::getLongName} for this method.
     *
     * @return long method name
     */
    default String getLongName() {
        return getTargetClass().getCanonicalName() + "::" + getTargetMethod().getName();
    }

    /**
     * Get the number of parameters the method has.
     *
     * @return Parameter count
     */
    default int getParameterCount() {
        return getTargetMethod().getParameterTypes().length;
    }
}
