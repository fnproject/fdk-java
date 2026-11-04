/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
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


import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class SuppliedFunctionInstanceTest {
    public static class Function {
        public String handle(String input) { return input; }
        public static String staticHandle(String input) { return input; }
    }

    @Test
    public void returnsOriginalInstanceWithoutConstructingAnother() {
        Function instance = new Function();
        FunctionRuntimeContext context = new FunctionRuntimeContext(
                new DefaultMethodWrapper(Function.class, "handle"), Collections.emptyMap(), instance);
        assertSame(instance, context.getInvokeInstance().get());
        assertSame(instance, context.getInvokeInstance().get());
    }

    @Test
    public void rejectsIncompatibleInstanceWithClassloaderGuidance() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new FunctionRuntimeContext(
                new DefaultMethodWrapper(Function.class, "handle"), Collections.emptyMap(), new Object()))
                .getMessage().contains("classloader"));
    }

    @Test
    public void rejectsStaticHandlerAndNullInstance() {
        assertThrows(IllegalArgumentException.class, () -> new FunctionRuntimeContext(
                new DefaultMethodWrapper(Function.class, "staticHandle"), Collections.emptyMap(), new Function()));
        assertThrows(NullPointerException.class, () -> new FunctionRuntimeContext(
                new DefaultMethodWrapper(Function.class, "handle"), Collections.emptyMap(), null));
    }
}
