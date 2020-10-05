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

import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.fn.testing.FunctionError;
import com.fnproject.fn.testing.flowtestfns.ExerciseEverything;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class IntegrationTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public FlowTesting flow = FlowTesting.create(fn);

    @Test
    public void runIntegrationTests() {

        flow.givenFn("testFunctionNonExistant")
          .withFunctionError()

          .givenFn("testFunction")
          .withAction((body) -> {
              if (new String(body).equals("PASS")) {
                  return "okay".getBytes();
              } else {
                  throw new FunctionError("failed as demanded");
              }
          });

        fn
          .givenEvent()
          .withBody("")   // or "1,5,6,32" to select a set of tests individually
          .enqueue()

          .thenRun(ExerciseEverything.class, "handleRequest");

        Assertions.assertThat(fn.getResults().get(0).getBodyAsString())
          .endsWith("Everything worked\n");
    }
}
