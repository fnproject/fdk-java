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

/**
 * Interface representing type information about a possibly-generic type.
 */
public interface TypeWrapper {

    /**
     * Unlike {@link java.lang.reflect.Method}, types (such as parameter types, or return types) are
     * resolved to a reified type even if they are generic, providing reification is possible.
     *
     * For example, take the following classes:
     * <pre>{@code
     * class GenericParent<T> {
     *   public void someMethod(T t) { // do something with t }
     * }
     *
     * class ConcreteClass extends GenericParent<String> { }
     * }</pre>
     *
     * A {@link TypeWrapper} representing the first argument of {@code someMethod} would return {@code String.class}
     * instead of {@code Object.class}
     *
     * @return Reified type
     */
    Class<?> getParameterClass();
}
