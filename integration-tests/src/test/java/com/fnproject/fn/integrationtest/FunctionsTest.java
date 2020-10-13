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

package com.fnproject.fn.integrationtest;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.integrationtest.IntegrationTestRule.CmdResult;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 14/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FunctionsTest {

    @Rule
    public final IntegrationTestRule testRule = new IntegrationTestRule();


    @Test
    public void shouldCallExistingFn() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/simpleFunc").rewritePOM();

        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "GREETING", "Salutations");

        CmdResult r1 = tc.runFnWithInput("", "invoke", tc.appName(), "simplefunc");
        assertThat(r1.getStdout()).isEqualTo("Salutations, world!");

        CmdResult r2 = tc.runFnWithInput("tests", "invoke", tc.appName(), "simplefunc");
        assertThat(r2.getStdout()).isEqualTo("Salutations, tests!");

    }

    @Test()
    public void checkBoilerPlate() throws Exception {
        for (String runtime : new String[] {"java8", "java11"}) {
            IntegrationTestRule.TestContext tc = testRule.newTest();
            String fnName = "bp" + runtime;
            tc.runFn("init", "--runtime", runtime, "--name", fnName);
            tc.rewritePOM();
            tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
            CmdResult rs = tc.runFnWithInput("wibble", "invoke", tc.appName(), fnName);
            assertThat(rs.getStdout()).contains("Hello, wibble!");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InspectResponse {

        public Map<String, String> annotations = new HashMap<>();
    }

    @Test()
    public void shouldHandleTrigger() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/httpgwfunc").rewritePOM();
        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");

        // Get me the trigger URL
        CmdResult output = tc.runFn("inspect", "trigger", tc.appName(), "httpgwfunc", "trig");

        ObjectMapper om = new ObjectMapper();
        InspectResponse resp = om.readValue(output.getStdout(), InspectResponse.class);
        String dest = resp.annotations.get("fnproject.io/trigger/httpEndpoint");
        assertThat(dest).withFailMessage("Missing trigger endpoint annotation").isNotNull();

        String url = dest + "?q1=a&q2=b";
        URL invokeURL = URI.create(url).toURL();

        System.out.println("calling " + url);
        HttpURLConnection con = (HttpURLConnection) invokeURL.openConnection();

        con.setRequestMethod("POST");
        con.addRequestProperty("Foo", "bar");


        assertThat(con.getResponseCode()).isEqualTo(202);
        assertThat(con.getHeaderField("GotMethod")).isEqualTo("POST");
        assertThat(con.getHeaderField("GotURL")).isEqualTo(url);
        assertThat(con.getHeaderField("GotHeader")).isEqualTo("bar");
        assertThat(con.getHeaderField("MyHTTPHeader")).isEqualTo("foo");

    }

    @Test
    public void shouldGetFDKVersion() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/simpleFunc").rewritePOM();

        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "GREETING", "Salutations");

        CmdResult r1 = tc.runFn("inspect", "function", tc.appName(), "simplefunc", "--endpoint");

        String url = r1.getStdout().trim();

        HttpURLConnection conn = (HttpURLConnection) (new URL(url).openConnection(Proxy.NO_PROXY));
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.getOutputStream().write(new byte[0]);
        Map<String, List<String>> headers = conn.getHeaderFields();

        assertThat(headers).hasEntrySatisfying("Fn-Fdk-Version", (val) -> {
            assertThat(val).isNotEmpty();
            assertThat(val.get(0)).matches("fdk-java/\\d+\\.\\d+\\.\\d+(-SNAPSHOT)? \\(jvm=.*, jvmv=.*\\)");
        });

    }

}
