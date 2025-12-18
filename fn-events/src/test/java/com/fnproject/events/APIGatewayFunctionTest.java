package com.fnproject.events;

import static org.junit.Assert.assertEquals;
import com.fnproject.events.testfns.apigatewayfns.APIGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.ListAPIGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.StringAPIGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.UncheckedAPIGatewayTestFunction;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class APIGatewayFunctionTest {

    @Rule
    public final FnTestingRule fnRule = FnTestingRule.createDefault();

    @Test
    public void testStringHandler() {
        fnRule
            .givenEvent()
            .withHeader("Fn-Http-H-Custom-Header", "headerValue")
            .withHeader("Fn-Http-Method", "POST")
            .withHeader("Fn-Http-Request-Url", "/v1?param1=value%20with%20spaces")
            .withBody("plain string body")
            .enqueue();

        fnRule.thenRun(StringAPIGatewayTestFunction.class, "handler");

        assertEquals("test response", fnRule.getOnlyResult().getBodyAsString());
        assertEquals("200", fnRule.getOnlyResult().getHeaders().get("Fn-Http-Status").get());
        assertEquals("headerValue", fnRule.getOnlyResult().getHeaders().get("Fn-Http-H-Custom-Header").get());
    }

    @Test
    public void testObjectHandler() {
        fnRule
            .givenEvent()
            .withHeader("Fn-Http-H-Custom-Header", "headerValue")
            .withHeader("Fn-Http-Method", "POST")
            .withHeader("Fn-Http-Request-Url", "/v1?param1=value%20with%20spaces")
            .withBody("{\"name\":\"chicken\",\"age\":2}")
            .enqueue();

        fnRule.thenRun(APIGatewayTestFunction.class, "handler");

        assertEquals("{\"brand\":\"ford\",\"wheels\":4}", fnRule.getOnlyResult().getBodyAsString());
    }

    @Test
    public void testNullObjectPropertyHandler() {
        fnRule
            .givenEvent()
            .withHeader("Fn-Http-H-Custom-Header", "headerValue")
            .withHeader("Fn-Http-Method", "POST")
            .withHeader("Fn-Http-Request-Url", "/v1?param1=value%20with%20spaces")
            .withBody("{\"age\":2}")
            .enqueue();

        fnRule.thenRun(APIGatewayTestFunction.class, "handler");

        assertEquals("{\"brand\":\"ford\",\"wheels\":4}", fnRule.getOnlyResult().getBodyAsString());
    }

    @Test
    public void testListObjectHandler() {
        fnRule
            .givenEvent()
            .withHeader("Fn-Http-H-Custom-Header", "headerValue")
            .withHeader("Fn-Http-Method", "POST")
            .withHeader("Fn-Http-Request-Url", "/v1?param1=value%20with%20spaces")
            .withBody("[{\"name\":\"chicken\",\"age\":2}]")
            .enqueue();

        fnRule.thenRun(ListAPIGatewayTestFunction.class, "handler");

        assertEquals("[{\"brand\":\"ford\",\"wheels\":4}]", fnRule.getOnlyResult().getBodyAsString());
    }

    @Test
    public void testUncheckedHandler() {
        fnRule
            .givenEvent()
            .withHeader("Fn-Http-H-Custom-Header", "headerValue")
            .withHeader("Fn-Http-Method", "POST")
            .withHeader("Fn-Http-Request-Url", "/v1?param1=value%20with%20spaces")
            .withBody("plain string body")
            .enqueue();

        fnRule.thenRun(UncheckedAPIGatewayTestFunction.class, "handler");

        assertEquals("test response", fnRule.getOnlyResult().getBodyAsString());
    }
}