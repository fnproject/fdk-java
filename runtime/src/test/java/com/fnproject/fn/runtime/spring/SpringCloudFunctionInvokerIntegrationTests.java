package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.*;
import com.fnproject.fn.runtime.spring.testfns.FunctionConfig;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.springframework.util.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerIntegrationTests {
    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void shouldInvokeFunction() throws IOException {
        fn.givenDefaultEvent().withBody("HELLO").enqueue();

        fn.thenRun(FunctionConfig.class, "handleRequest");

        String output = fn.getStdOutAsString();
        assertThat(output).isEqualTo("hello");
    }

    @Test
    public void shouldInvokeConsumer() throws IOException {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_CONSUMER_NAME, "consumer");
        String consumerInput = "consumer input";
        fn.givenDefaultEvent().withBody(consumerInput).enqueue();

        fn.thenRun(FunctionConfig.class, "handleRequest");

        assertThat(fn.getStdErrAsString()).contains(consumerInput);
    }

    @Test
    public void shouldInvokeSupplier() throws IOException {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_SUPPLIER_NAME, "supplier");
        fn.givenDefaultEvent().enqueue();

        fn.thenRun(FunctionConfig.class, "handleRequest");

        String output = fn.getStdOutAsString();
        assertThat(output).isEqualTo("Hello");
    }
}

