package com.fnproject.fn.integrationtest;

import com.fnproject.fn.integrationtest.IntegrationTestRule.CmdResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 14/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FlowTest {

    @Rule
    public final IntegrationTestRule testRule = new IntegrationTestRule();


    @Test
    public void shouldInvokeBasicFlow() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/flowBasic").rewritePOM();
        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "COMPLETER_BASE_URL", testRule.getFlowURL());
        CmdResult r = tc.runFnWithInput("1", "invoke", tc.appName(), "flowbasic");
        assertThat(r.getStdout()).isEqualTo("4");
    }


    @Test
    public void shouldInvokeBasicFlowJDK8() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/flowBasicJDK8").rewritePOM();
        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "COMPLETER_BASE_URL", testRule.getFlowURL());
        CmdResult r = tc.runFnWithInput("1", "invoke", tc.appName(), "flowbasicj8");
        assertThat(r.getStdout()).isEqualTo("4");
    }


    @Test
    public void shouldExerciseAllFlow() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/flowAllFeatures").rewritePOM();
        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "COMPLETER_BASE_URL", testRule.getFlowURL());
        CmdResult r = tc.runFnWithInput("1", "invoke", tc.appName(), "flowallfeatures");
        assertThat(r.getStdout()).contains("Everything worked");
    }


    @Test
    public void shouldCallExitHooks() throws Exception {
        CompletableFuture<Boolean> done = new CompletableFuture<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/exited", httpExchange -> {
            done.complete(true);
            String resp = "ok";
            httpExchange.sendResponseHeaders(200, resp.length());
            httpExchange.getResponseBody().write(resp.getBytes());
            httpExchange.getResponseBody().close();
        });
        server.setExecutor(null); // creates a default executor
        server.start();
        try {
            IntegrationTestRule.TestContext tc = testRule.newTest();
            tc.withDirFrom("funcs/flowExitHooks").rewritePOM();
            tc.runFn("--verbose", "build", "--no-cache");
            tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
            tc.runFn("config", "app", tc.appName(), "COMPLETER_BASE_URL", testRule.getFlowURL());
            tc.runFn("config", "app", tc.appName(), "TERMINATION_HOOK_URL", "http://" + testRule.getDockerLocalhost() + ":" + 8000 + "/exited");
            CmdResult r = tc.runFnWithInput("1", "invoke", tc.appName(), "flowexithooks");
            assertThat(r.getStdout()).contains("42");

            assertThat(done.get(10, TimeUnit.SECONDS)).withFailMessage("Expected callback within 10 seconds").isTrue();

        } finally {
            server.stop(0);
        }
    }


    @Test
    public void shouldHandleTimeouts() throws Exception {
        IntegrationTestRule.TestContext tc = testRule.newTest();
        tc.withDirFrom("funcs/flowTimeouts").rewritePOM();
        tc.runFn("--verbose", "deploy", "--create-app", "--app", tc.appName(), "--local");
        tc.runFn("config", "app", tc.appName(), "COMPLETER_BASE_URL", testRule.getFlowURL());
        CmdResult r = tc.runFn("invoke", tc.appName(), "flowtimeouts");
        assertThat(r.getStdout()).contains("timeout");
    }

}
