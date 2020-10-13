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

package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.EventCodec;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testing classloader that loads all classes afresh when needed, otherwise delegates shared classes to the parent classloader
 */
public class FnTestingClassLoader extends ClassLoader {
    private final List<String> sharedPrefixes;
    private final Map<String, Class<?>> loaded = new HashMap<>();

    public FnTestingClassLoader(ClassLoader parent, List<String> sharedPrefixes) {
        super(parent);
        this.sharedPrefixes = sharedPrefixes;
    }

    boolean isShared(String classOrPackageName) {
        for (String prefix : sharedPrefixes) {
            if (("=" + classOrPackageName).equals(prefix) || classOrPackageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> definedClass = loaded.get(className);
        if (definedClass != null) {
            return definedClass;
        }

        Class<?> cls = null;
        if (isShared(className)) {
            cls = getParent().loadClass(className);
        }

        if (cls == null) {
            try {
                InputStream in = getResourceAsStream(className.replace('.', '/') + ".class");
                if (in == null) {
                    throw new ClassNotFoundException("Class not found :" + className);
                }

                byte[] clsBytes = IOUtils.toByteArray(in);
                cls = defineClass(className, clsBytes, 0, clsBytes.length);
                resolveClass(cls);

            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        }
        loaded.put(className, cls);
        return cls;
    }


    public int run(Map<String, String> mutableEnv, EventCodec codec, PrintStream functionErr, String... s) {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {


            Thread.currentThread().setContextClassLoader(this);

            Class<?> entryPoint_class = loadClass(EntryPoint.class.getName());
            Object entryPoint = entryPoint_class.newInstance();

            return (int) getMethodInClassLoader(entryPoint, "run", Map.class, EventCodec.class, PrintStream.class, String[].class)
              .invoke(entryPoint, mutableEnv, codec, functionErr, s);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private Method getMethodInClassLoader(Object target, String method, Class... types) throws NoSuchMethodException {
        Class<?> targetClass;
        if (target instanceof Class) {
            targetClass = (Class) target;
        } else {
            targetClass = target.getClass();
        }
        return targetClass.getMethod(method, types);
    }
}