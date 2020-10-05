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

package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.RuntimeContext;

import java.util.Objects;

public class TestFnConstructors {

    public static class DefaultEmptyConstructor {
        public String invoke() {
            return "OK";
        }
    }

    public static class ExplicitEmptyConstructor {

        public ExplicitEmptyConstructor() {
        }

        public String invoke() {
            return "OK";
        }

    }

    public static class ConfigurationOnConstructor {

        final RuntimeContext ctx;

        public ConfigurationOnConstructor(RuntimeContext ctx) {
            this.ctx = Objects.requireNonNull(ctx);

        }

        public String invoke() {
            Objects.requireNonNull(ctx);
            return "OK";
        }

    }

    public static class BadConstructorThrowsException {

        public BadConstructorThrowsException() {
            throw new RuntimeException("error in constructor");
        }

        public String invoke() {
            throw new RuntimeException("should not run");

        }
    }

    public static class BadConstructorUnrecognisedArg {

        public BadConstructorUnrecognisedArg(int x) {

        }

        public String invoke() {
            throw new RuntimeException("should not run");

        }
    }


    public static class BadConstructorAmbiguousConstructors {

        public BadConstructorAmbiguousConstructors(RuntimeContext x) {
        }

        public BadConstructorAmbiguousConstructors() {
        }

        public String invoke() {
            throw new RuntimeException("should not run");

        }
    }

    public static class BadConstructorTooManyArgs {

        public BadConstructorTooManyArgs(RuntimeContext ctx, int x) {
        }

        public String invoke() {
            throw new RuntimeException("should not run");

        }
    }

    private static abstract class BadClassAbstract {
        public String invoke() {
            throw new RuntimeException("should not run");
        }
    }

    public static class BadConstructorNotAccessible {

        private BadConstructorNotAccessible() {
        }

        public String invoke() {
            throw new RuntimeException("should not run");
        }
    }


    public class NonStaticInnerClass {
        public String invoke(String x) {
            throw new RuntimeException("should not run");
        }
    }
}
