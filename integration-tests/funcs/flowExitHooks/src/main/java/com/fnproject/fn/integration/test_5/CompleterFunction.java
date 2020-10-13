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

package com.fnproject.fn.integration.test_5;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@FnFeature(FlowFeature.class)
public class CompleterFunction implements Serializable {

    private final URL callbackURL;

    public CompleterFunction(RuntimeContext rtc) throws Exception {
        callbackURL = URI.create(rtc.getConfigurationByKey("TERMINATION_HOOK_URL").orElseThrow(() -> new RuntimeException("No config set "))).toURL();
    }

    public Integer handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        fl.addTerminationHook((ignored) -> {
            try {
                HttpURLConnection con = (HttpURLConnection) callbackURL.openConnection();

                System.err.println("Ran the hook.");

                con.setRequestMethod("GET");
                if (con.getResponseCode() != 200) {
                    throw new RuntimeException("Got bad code from callback");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        return fl.supply(() -> {
            return 42;
        }).get();
    }

}
