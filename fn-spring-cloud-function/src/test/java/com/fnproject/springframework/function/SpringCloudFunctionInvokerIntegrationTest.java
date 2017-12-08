package com.fnproject.springframework.function;

import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.springframework.function.testfns.EmptyFunctionConfig;
import com.fnproject.springframework.function.testfns.FunctionConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


public class SpringCloudFunctionInvokerIntegrationTest {

    @Rule
    public final FnTestingRule fnRule = FnTestingRule.createDefault();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void shouldInvokeFunction() throws IOException {

        fnRule.givenEvent().withBody("HELLO").enqueue();
        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        assertThat(fnRule.getOnlyResult().getBodyAsString()).isEqualTo("hello");
    }

    @Test
    public void shouldInvokeConsumer() throws IOException {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_CONSUMER_NAME, "consumer");
        String consumerInput = "consumer input";
        fnRule.givenEvent().withBody(consumerInput).enqueue();

        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        assertThat(fnRule.getStdErrAsString()).contains(consumerInput);
    }

    @Test
    public void shouldInvokeSupplier() throws IOException {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_SUPPLIER_NAME, "supplier");
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(FunctionConfig.class, "handleRequest");

        String output = fnRule.getOnlyResult().getBodyAsString();
        assertThat(output).isEqualTo("Hello");
    }

    @Test
    public void shouldThrowFunctionLoadExceptionIfNoValidFunction() {
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(EmptyFunctionConfig.class, "handleRequest");

        int exitCode = fnRule.getLastExitCode();

        assertThat(exitCode).isEqualTo(2);
        assertThat(fnRule.getResults()).isEmpty(); // fails at init so no results.
        assertThat(fnRule.getStdErrAsString()).contains("No Spring Cloud Function found");
    }

    @Test
    public void noNPEifFunctionReturnsNull() {
        fnRule.givenEvent().enqueue();

        fnRule.thenRun(EmptyFunctionConfig.class, "handleRequest");

        int exitCode = fnRule.getLastExitCode();

        assertThat(exitCode).isEqualTo(2);
        assertThat(fnRule.getResults()).isEmpty(); // fails at init so no results.
        assertThat(fnRule.getStdErrAsString()).contains("No Spring Cloud Function found");
    }
}

