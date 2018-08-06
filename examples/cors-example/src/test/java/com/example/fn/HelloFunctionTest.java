package com.example.fn;

import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HelloFunctionTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldHandleOptionsRequest() {
        testing.givenEvent().withMethod("OPTIONS").enqueue();
        testing.thenRun(CorsFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("", result.getBodyAsString());
    }

    @Test
    public void shouldHandleGet() {
        testing.givenEvent().withMethod("GET").enqueue();
        testing.thenRun(CorsFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Hello, world!", result.getBodyAsString());

    }

}