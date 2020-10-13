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

package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.RuntimeFeature;

/**
 *
 * The flow feature enables the Flow Client SDK and runtime behaviour in a Java function in order to use Flow in a function you must add the following to the function class:
 *
 *
 * <code>
 * import com.fnproject.fn.api.FnFeature;
 * import com.fnproject.fn.runtime.flow.FlowFeature;
 *
 * @FnFeature(FlowFeature.class)
 * public class MyFunction {
 *
 *
 *     public void myFunction(String input){
 *         Flows.currentFlow()....
 *
 *     }
 * }
 *
 * </code>
 *
 * Created on 10/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FlowFeature implements RuntimeFeature {
    @Override
    public void initialize(RuntimeContext context){
        FunctionInvoker invoker = new FlowContinuationInvoker();
        context.addInvoker(invoker,FunctionInvoker.Phase.PreCall);
    }
}
