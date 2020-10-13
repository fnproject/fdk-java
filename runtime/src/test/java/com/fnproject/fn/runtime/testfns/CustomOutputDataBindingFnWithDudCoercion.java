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

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.testfns.coercions.DudCoercion;
import com.fnproject.fn.runtime.testfns.coercions.StringReversalCoercion;

public class CustomOutputDataBindingFnWithDudCoercion {
    @FnConfiguration
    public static void outputConfig(RuntimeContext ctx){
        ctx.addOutputCoercion(new DudCoercion());
        ctx.addOutputCoercion(new StringReversalCoercion());
    }

    public String echo(String s){ return s; }
}
