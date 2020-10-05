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

package com.fnproject.fn.integration.test_6;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@FnFeature(FlowFeature.class)
public class CompleterFunction {

    public String handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        try {
            return fl.supply(() -> {
                Thread.sleep(10000);
                return "nope";
            }).get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            return "timeout";
        }
    }

}
