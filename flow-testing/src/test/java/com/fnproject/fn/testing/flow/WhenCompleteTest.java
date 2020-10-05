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

package com.fnproject.fn.testing.flow;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;
import com.fnproject.fn.testing.FnTestingRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class WhenCompleteTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();
    private FlowTesting flow = FlowTesting.create(fn);

    public static AtomicInteger cas = new AtomicInteger(0);


    @FnFeature(FlowFeature.class)
    public static class TestFn {
        public void handleRequest() {
            Flows.currentFlow().completedValue(1)
                    .whenComplete((v, e) -> WhenCompleteTest.cas.compareAndSet(0, 1))
                    .thenRun(() -> WhenCompleteTest.cas.compareAndSet(1, 2));
        }
    }

    @Test
    public void OverlappingFlowInvocationsShouldWork() {
        fn.addSharedClass(WhenCompleteTest.class);

        cas.set(0);

        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "handleRequest");

        Assertions.assertThat(cas.get()).isEqualTo(2);
    }

}
