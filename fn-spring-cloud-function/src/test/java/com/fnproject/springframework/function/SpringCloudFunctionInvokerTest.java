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

import com.fnproject.springframework.function.functions.SpringCloudFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerTest {
    private SpringCloudFunctionInvoker invoker;

    @Before
    public void setUp() {
        invoker = new SpringCloudFunctionInvoker((SpringCloudFunctionLoader) null);
    }

    @Test
    public void invokesFunctionWithEmptyFlux() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[0]);

        assertThat(result).isNull();
    }

    @Test
    public void invokesFunctionWithFluxOfSingleItem() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[]{ "hello" });

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    public void invokesFunctionWithFluxOfMultipleItems() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[]{ Arrays.asList("hello", "world") });

        assertThat(result).isInstanceOf(List.class);
        assertThat((List) result).containsSequence("hello", "world");
    }

}
