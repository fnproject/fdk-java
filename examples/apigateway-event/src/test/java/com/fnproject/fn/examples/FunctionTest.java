package com.fnproject.fn.examples;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.Collections;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.events.testing.APIGatewayTestFeature;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class FunctionTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    private final APIGatewayTestFeature apiGatewayFeature = APIGatewayTestFeature.createDefault(fn);

    @Test
    public void testGetResponseBody() throws IOException {
        APIGatewayRequestEvent<RequestEmployee> event = createMinimalRequest();

        apiGatewayFeature.givenEvent(event)
            .enqueue();

        fn.thenRun(Function.class, "handler");

        APIGatewayResponseEvent<ResponseEmployee> responseEvent = apiGatewayFeature.getResult(ResponseEmployee.class);

        ResponseEmployee responseEventBody = responseEvent.getBody();
        assertEquals(Integer.valueOf(123), responseEventBody.getId());
        assertEquals("John", responseEventBody.getName());
    }

    @Test
    public void testGetResponseHeaders() throws IOException {
        APIGatewayRequestEvent<RequestEmployee> event = createMinimalRequest();

        when(event.getHeaders()).thenReturn(Headers.emptyHeaders().addHeader("myHeader", "headerValue"));

        apiGatewayFeature.givenEvent(event)
            .enqueue();

        fn.thenRun(Function.class, "handler");

        APIGatewayResponseEvent<ResponseEmployee> responseEvent = apiGatewayFeature.getResult(ResponseEmployee.class);
        assertEquals("HeaderValue", responseEvent.getHeaders().getAllValues("X-Custom-Header").get(0));
        assertEquals("HeaderValue2", responseEvent.getHeaders().get("X-Custom-Header-2").get());
    }

    @Test
    public void testGetResponseStatus() throws IOException {
        APIGatewayRequestEvent<RequestEmployee> event = createMinimalRequest();

        apiGatewayFeature.givenEvent(event)
            .enqueue();

        fn.thenRun(Function.class, "handler");

        APIGatewayResponseEvent<ResponseEmployee> responseEvent = apiGatewayFeature.getResult(ResponseEmployee.class);

        assertEquals(Integer.valueOf(201), responseEvent.getStatus());
    }

    @Test
    public void testErrorResponse() throws IOException {
        APIGatewayRequestEvent<RequestEmployee> event = mock(APIGatewayRequestEvent.class);

        apiGatewayFeature.givenEvent(event)
            .enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(502, result.getStatus().getCode());
        assertEquals("An error occurred in function: requestEmployee must not be null\n" +
            "Caused by: java.lang.IllegalArgumentException: requestEmployee must not be null\n\n", fn.getStdErrAsString());
        assertEquals(1, fn.getLastExitCode());
    }

    private static APIGatewayRequestEvent<RequestEmployee> createMinimalRequest() {
        RequestEmployee requestEmployee = new RequestEmployee();
        requestEmployee.setName("John");
        APIGatewayRequestEvent<RequestEmployee> event = mock(APIGatewayRequestEvent.class);

        when(event.getBody()).thenReturn(requestEmployee);
        when(event.getQueryParameters()).thenReturn(new QueryParametersImpl(Collections.singletonMap("id", Collections.singletonList("123"))));
        return event;
    }
}