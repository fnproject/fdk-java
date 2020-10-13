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

import java.util.HashMap;
import java.util.Map;

public class PrimitiveTypeResolver {
    private static final Map<Class<?>, Class<?>> boxedTypes = new HashMap<>();
    static {
        boxedTypes.put(void.class, Void.class);
        boxedTypes.put(boolean.class, Boolean.class);
        boxedTypes.put(byte.class, Byte.class);
        boxedTypes.put(char.class, Character.class);
        boxedTypes.put(short.class, Short.class);
        boxedTypes.put(int.class, Integer.class);
        boxedTypes.put(long.class, Long.class);
        boxedTypes.put(float.class, Float.class);
        boxedTypes.put(double.class, Double.class);
    }

    /**
     * Resolves cls from a possibly primitive class to a boxed type otherwise just returns cls
     */
    public static Class<?> resolve(Class<?> cls) {
        if (cls.isPrimitive()) {
            return boxedTypes.get(cls);
        }
        return cls;
    }
}
