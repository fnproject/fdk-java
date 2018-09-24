package com.example.fn;

import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TriggerFunctionTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldReturnGreeting() {
        testing.givenEvent().enqueue();
        testing.thenRun(TriggerFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Hello, world!", result.getBodyAsString());
    }


    @Test
    public void shouldWorkWithHTTP() {
        testing.givenEvent()
          .withHeader("Fn-Http-Method", "POST")
          .withHeader("Fn-Http-H-Foo", "bar")
          .withHeader("Fn-Http-Request-Url", "http://mysite.com/?q1=2&q3=4")
          .enqueue();
        testing.thenRun(TriggerFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Hello, world!", result.getBodyAsString());
        assertEquals("202", result.getHeaders().get("Fn-Http-Status").orElse(""));
    }


}