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
import com.fnproject.fn.api.TypeWrapper;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Wrapper class around {@link java.lang.reflect.Method} to provide type
 * resolution for methods with generic arguments
 */
public class DefaultMethodWrapper implements MethodWrapper {
    private final Class<?> srcClass;
    private final Method srcMethod;

    DefaultMethodWrapper(Class<?> srcClass, Method srcMethod) {
        this.srcClass = srcClass;
        this.srcMethod = srcMethod;
    }

    DefaultMethodWrapper(Class<?> srcClass, String srcMethod) {
        this(srcClass, Arrays.stream(srcClass.getMethods())
          .filter((m) -> m.getName().equals(srcMethod))
          .findFirst()
          .orElseThrow(() -> new RuntimeException(new NoSuchMethodException(srcClass.getCanonicalName() + "::" + srcMethod))));
    }


    @Override
    public Class<?> getTargetClass() {
        return srcClass;
    }

    @Override
    public Method getTargetMethod() {
        return srcMethod;
    }

    @Override
    public TypeWrapper getParamType(int index) {
        return MethodTypeWrapper.fromParameter(this, index);
    }

    @Override
    public TypeWrapper getReturnType() {
        return MethodTypeWrapper.fromReturnType(this);
    }

    @Override
    public String toString() {
        return getLongName();
    }
}

