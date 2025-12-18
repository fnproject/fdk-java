package com.fnproject.events.testing;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl;
import com.fnproject.fn.testing.FnEventBuilderJUnit4;
import com.fnproject.fn.testing.FnTestingRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

public class APIGatewayTestFeatureTest {

    @Rule
    public final FnTestingRule fn = FnTestingRule.createDefault();

    APIGatewayTestFeature feature = APIGatewayTestFeature.createDefault(fn);

    public static class Body {
        public final String message;

        @JsonCreator
        public Body(@JsonProperty("message") String message) {
            this.message = message;
        }
    }

/*
    A minimal function that echoes input and captures request.
*/
    public String handle(HTTPGatewayContext ctx, InputEvent inputEvent) {

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string", e);
            }
        });

        ctx.getHeaders().asMap().forEach((key, value1) -> value1.forEach(value -> {
            ctx.addResponseHeader(key, value);
        }));

        ctx.setStatusCode(200);

        return body;
    }

    @Test
    public void testRequestStringifyBody() throws Exception {
        Body reqBody = new Body("hello");
        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), reqBody, "GET", "/v2/employee", Headers.emptyHeaders());

        APIGatewayTestFeature.APIGatewayFnEventBuilder builder = (APIGatewayTestFeature.APIGatewayFnEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
                try {
                    return IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading input as string");
                }
            });
        assertEquals("{\"message\":\"hello\"}", body);
    }

    @Test
    public void testRequestUrl() throws Exception {
        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), null, "GET", "/v2/employee", Headers.emptyHeaders());

        APIGatewayTestFeature.APIGatewayFnEventBuilder builder = (APIGatewayTestFeature.APIGatewayFnEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals(req.getRequestUrl(), inputEvent.getHeaders().get("Fn-Http-Request-Url").get());
    }

    @Test
    public void testRequestHeaderMethod() throws Exception {
        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), null, "GET", "/v2/employee", Headers.emptyHeaders());

        APIGatewayTestFeature.APIGatewayFnEventBuilder builder = (APIGatewayTestFeature.APIGatewayFnEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals(req.getMethod(), inputEvent.getHeaders().get("Fn-Http-Method").get());
    }

    @Test
    public void testRequestQueryParameters() throws Exception {
        Map<String, List<String>> queryParams = new HashMap<>();
        List<String> paramValues = new ArrayList<>();
        paramValues.add("bar");
        paramValues.add("foo");
        queryParams.put("foo", paramValues);
        queryParams.put("spaces", Collections.singletonList("this has spaces"));

        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(queryParams), null, "GET", "/v2/employee", Headers.emptyHeaders());

        APIGatewayTestFeature.APIGatewayFnEventBuilder builder = (APIGatewayTestFeature.APIGatewayFnEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals("/v2/employee?spaces=this+has+spaces&foo=bar&foo=foo", inputEvent.getHeaders().get("Fn-Http-Request-Url").get());
    }

    @Test
    public void testReturnObjectResponse() throws Exception {
        Body reqBody = new Body("hello");
        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), reqBody, "GET", "/v2/employee", Headers.emptyHeaders());

        FnEventBuilderJUnit4 builder = feature.givenEvent(req);
        builder.enqueue();

        fn.thenRun(APIGatewayTestFeatureTest.class, "handle");

        APIGatewayResponseEvent<Body> resp = feature.getResult(Body.class);

        assertNotNull(resp.getBody());
        assertEquals("hello", resp.getBody().message);
    }

    @Test
    public void testReturnNullObjectResponse() throws Exception {
        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), null, "GET", "/v2/employee", Headers.emptyHeaders());

        FnEventBuilderJUnit4 builder = feature.givenEvent(req);
        builder.enqueue();

        fn.thenRun(APIGatewayTestFeatureTest.class, "handle");

        APIGatewayResponseEvent<Body> resp = feature.getResult(Body.class);

        assertNull(resp.getBody());
    }

    @Test
    public void testReturnHeaderResponse() throws Exception {
        Headers headers = Headers.emptyHeaders()
            .addHeader("Custom", "foo")
            .addHeader("Custom", "bar");

        APIGatewayRequestEvent req = new APIGatewayRequestEvent(new QueryParametersImpl(), null, "GET", "/v2/employee", headers);

        FnEventBuilderJUnit4 builder = feature.givenEvent(req);
        builder.enqueue();

        fn.thenRun(APIGatewayTestFeatureTest.class, "handle");

        APIGatewayResponseEvent<Body> resp = feature.getResult(Body.class);

        assertNotNull(resp.getHeaders());
        assertEquals("foo", resp.getHeaders().getAllValues("Custom").get(0));
        assertEquals("bar", resp.getHeaders().getAllValues("Custom").get(1));
    }

} 