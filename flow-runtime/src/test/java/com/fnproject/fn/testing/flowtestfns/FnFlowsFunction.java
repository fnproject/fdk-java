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

package com.fnproject.fn.testing.flowtestfns;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

import java.io.Serializable;

@FnFeature(FlowFeature.class)
public class FnFlowsFunction implements Serializable {

    public static void usingFlows() {
        Flows.currentFlow();
    }

    public static void notUsingFlows() {
    }

    public static void supply() {
        Flow fl = Flows.currentFlow();
        fl.supply(() -> 3);
    }

    public static void accessRuntimeMultipleTimes() {
        Flows.currentFlow();
        Flows.currentFlow();
    }

    public static Integer supplyAndGetResult() {
        Flow fl = Flows.currentFlow();
        Integer res =  fl.supply(() -> 3).get();

        return res;
    }

    public static void createFlowAndThenFail() {
        Flow fl = Flows.currentFlow();
        throw new NullPointerException(fl.toString());
    }
}

