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

package com.fnproject.fn.testing;

import java.io.PrintStream;

/**
 * Created on 07/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface FnTestingRuleFeature {

    /**
     * Prepares a test
     * @param functionClassLoader
     * @param stderr
     * @param cls
     * @param method
     */
    void prepareTest(ClassLoader functionClassLoader, PrintStream stderr, String cls, String method);


    void prepareFunctionClassLoader(FnTestingClassLoader cl);


    void afterTestComplete();
}
