package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.*;
import com.fnproject.fn.runtime.spring.testfns.FunctionConfig;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.util.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerTests {
    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @Test
    public void shouldInvokeFunction() throws IOException {
        fn.givenDefaultEvent().withBody("HELLO").enqueue();

        fn.thenRun(FunctionConfig.class, "handleRequest");

        String output = fn.getStdOutAsString();
        assertThat(output).isEqualTo("hello");
    }
}

