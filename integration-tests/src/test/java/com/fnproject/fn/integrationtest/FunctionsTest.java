package com.fnproject.fn.integrationtest;

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

        tc.runFn("--verbose", "deploy", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "GREETING", "Salutations");


        CmdResult r1 = tc.runFnWithInput("", "invoke", tc.appName(), "simplefunc");
        assertThat(r1.getStdout()).isEqualTo("Salutations, world!");


        CmdResult r2 = tc.runFnWithInput("tests", "invoke", tc.appName(), "simplefunc");
        assertThat(r2.getStdout()).isEqualTo("Salutations, tests!");

    }

    @Test()
    public void checkBoilerPlate() throws Exception {
        for (String format : new String[]{"default", "http", "http-stream"}) {
            for (String runtime : new String[]{"java9", "java8"}) {
                IntegrationTestRule.TestContext tc = testRule.newTest();
                String fnName = "bp" + format + runtime;

                tc.runFn("init", "--runtime", runtime, "--name", fnName,"--format", format);
                tc.rewritePOM();
                tc.runFn("--verbose", "deploy", "--app", tc.appName(), "--local");
                CmdResult rs = tc.runFnWithInput("wibble", "invoke", tc.appName(), fnName);
                assertThat(rs.getStdout()).contains("Hello, wibble!");
            }
        }
    }

}
