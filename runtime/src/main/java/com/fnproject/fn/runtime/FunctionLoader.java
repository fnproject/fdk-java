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

package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.runtime.exception.InvalidFunctionDefinitionException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionLoader {
    private static ClassLoader contextClassLoader = FunctionLoader.class.getClassLoader();

    /**
     * create a function runtime context for a given class and method name
     *
     * @param className the class name to load
     * @param fnName    the function in the class
     * @return a new runtime context
     */
    public MethodWrapper loadClass(String className, String fnName) {
        Class<?> targetClass = loadClass(className);


        return new DefaultMethodWrapper(targetClass, getTargetMethod(targetClass, fnName));
    }

    private Method getTargetMethod(Class<?> targetClass, String method) {
        List<Method> namedMethods = findMethodsByName(method, targetClass);

        if (namedMethods.isEmpty()) {
            String names = Arrays.stream(targetClass.getDeclaredMethods())
                    .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                    .map(Method::getName)
                    .filter((name) -> !name.startsWith("$"))
                    .reduce((x, y) -> (x + "," + y)).orElseGet(() -> "");

            throw new InvalidFunctionDefinitionException("Method '" + method + "' was not found " +
                    "in class '" + targetClass.getCanonicalName() + "'. Available functions were: [" + names + "]");
        }

        if (namedMethods.size() > 1) {
            throw new InvalidFunctionDefinitionException("Multiple methods match name " + method + " in " + targetClass.getCanonicalName() + "  matching methods were [" + namedMethods.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        return namedMethods.get(0);
    }

    private List<Method> findMethodsByName(String fnName, Class<?> fnClass) {
        return Arrays.stream(fnClass.getMethods())
                .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> fnName.equals(m.getName()))
                .filter(m -> !m.isBridge())
                .collect(Collectors.toList());
    }

    private Class<?> loadClass(String className) {
        Class<?> fnClass;
        try {
            fnClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new InvalidFunctionDefinitionException(String.format("Class '%s' not found in function jar. " +
                    "It's likely that the 'cmd' entry in func.yaml is incorrect.", className));
        }
        return fnClass;
    }

    /**
     * Override the classloader used for fn class resolution
     * Primarily for testing, otherwise the system/default  classloader is used.
     *
     * @param loader the context class loader to use for this function
     */
    public static void setContextClassLoader(ClassLoader loader) {
        contextClassLoader = loader;
    }
}
