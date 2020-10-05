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

package com.fnproject.fn.api.flow;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowsTest {
    public FlowsTest() {
    }

    /** People shall not be allowed to create subclasses of {@code Flow}:
    * <pre>
    * static class MyFlows extends Flows {
    * }
    * </pre>
    */
    @Test
    public void dontSubclassFlows() {
        assertTrue("Flows is final", Modifier.isFinal(Flows.class.getModifiers()));
        assertEquals("No visible constructors", 0, Flows.class.getConstructors().length);
    }
}
