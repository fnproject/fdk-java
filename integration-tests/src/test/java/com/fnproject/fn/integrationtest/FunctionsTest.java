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
    public void shouldCallFnAllFormats() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/simpleFunc").rewritePom();

        tc.runFn("--verbose", "deploy", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "GREETING", "Salutations");

        for (String format : new String[]{"http", "default"
          // ,"http-stream"
        }) {

            tc.runFn("update", "function", tc.appName(), "test", "--format", format).assertNoError();

            CmdResult r1 = tc.runFnWithInput("", "invoke", tc.appName(), "test").assertNoError();
            assertThat(r1.getStdout()).isEqualTo("Salutations, world!");


            CmdResult r2 = tc.runFnWithInput("tests", "invoke", tc.appName(), "test").assertNoError();
            assertThat(r2.getStdout()).isEqualTo("Salutations, tests!");
        }
    }

    @Test
    public void checkBoilerPlate() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.runFn("init", "--runtime", "java", "--name","test","--trigger","none").assertNoError();
        tc.runFn("--verbose", "deploy", "--app", tc.appName(), "--local").assertNoError();
        tc.runFnWithInput("", "invoke", tc.appName(), "test").assertNoError();

    }


}
