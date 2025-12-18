package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.APIGatewayCoercion.OM_KEY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.events.testfns.Car;
import com.fnproject.events.testfns.apigatewayfns.APIGatewayTestFunction;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.apigatewayfns.GrandChildGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.ListAPIGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.StringAPIGatewayTestFunction;
import com.fnproject.events.testfns.apigatewayfns.UncheckedAPIGatewayTestFunction;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.DefaultFunctionInvocationContext;
import com.fnproject.fn.runtime.DefaultMethodWrapper;
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import org.junit.Before;
import org.junit.Test;


public class APIGatewayCoercionTest {
    private APIGatewayCoercion coercion;
    private InvocationContext requestinvocationContext;
    private DefaultFunctionInvocationContext responseInvocationContext;

    @Before
    public void setUp() {
        coercion = APIGatewayCoercion.instance();
        requestinvocationContext = mock(InvocationContext.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        ObjectMapper mapper = new ObjectMapper();

        when(runtimeContext.getAttribute(OM_KEY, ObjectMapper.class)).thenReturn(Optional.of(mapper));
        when(requestinvocationContext.getRuntimeContext()).thenReturn(runtimeContext);
    }

    @Test
    public void testReturnEmptyWhenNotAPIGatewayClass() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayCoercionTest.class, "testMethod");

        Headers headers = Headers.emptyHeaders();

        when(requestinvocationContext.getRequestHeaders()).thenReturn(headers);
        ByteArrayInputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        Optional<APIGatewayRequestEvent> requestEvent = coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method);

        assertFalse(requestEvent.isPresent());
    }

    @Test
    public void testRequestHeaders() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");

        Headers h = Headers.emptyHeaders()
            .setHeader("H1", "h1val")
            .setHeader("Fn-Http-H-", "ignored")
            .setHeader("Fn-Http-H-A", "b")
            .setHeader("Fn-Http-H-mv", "c", "d");

        APIGatewayRequestEvent<String> requestEvent = coerceRequest(method, h, "");

        assertEquals("b", (requestEvent.getHeaders().get("A")).get());
        assertEquals("c", (requestEvent.getHeaders().getAllValues("Mv")).get(0));
        assertEquals("d", (requestEvent.getHeaders().getAllValues("Mv")).get(1));
        assertFalse(requestEvent.getHeaders().get("H1").isPresent());
    }

    @Test
    public void testRequestUrl() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");

        Headers h = Headers.emptyHeaders()
            .setHeader("Fn-Http-Request-Url", "/v2/employee/123?param1=value%20with%20spaces");

        APIGatewayRequestEvent<String> requestEvent = coerceRequest(method, h, "");

        assertEquals("/v2/employee/123?param1=value%20with%20spaces", requestEvent.getRequestUrl());
    }

    @Test
    public void testRequestMethod() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");

        Headers h = Headers.emptyHeaders()
            .setHeader("Fn-Http-Method", "PATCH");

        APIGatewayRequestEvent requestEvent = coerceRequest(method, h, "");

        assertEquals("PATCH", requestEvent.getMethod());
    }

    @Test
    public void testQueryParameters() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");

        Headers h = Headers.emptyHeaders()
            .setHeader("Fn-Http-Request-Url", "/v2/employee/123?param1=value%20with%20spaces&repeat=2&repeat=3");

        APIGatewayRequestEvent requestEvent = coerceRequest(method, h, "");

        assertEquals("value with spaces", requestEvent.getQueryParameters().get("param1").get());
    }

    @Test
    public void testQueryRepeatedParameters() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");

        Headers h = Headers.emptyHeaders()
            .setHeader("Fn-Http-Request-Url", "/v2/employee/123?param1=value%20with%20spaces&repeat=2&repeat=3");

        APIGatewayRequestEvent requestEvent = coerceRequest(method, h, "");

        assertEquals("2", (requestEvent.getQueryParameters().getValues("repeat")).get(0));
        assertEquals("3", (requestEvent.getQueryParameters().getValues("repeat")).get(1));
    }

    @Test
    public void testRequestStringBody() {
        MethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");
        APIGatewayRequestEvent requestEvent = coerceRequest(method, "simple string");

        assertEquals("simple string", requestEvent.getBody());
    }

    @Test
    public void testListObjectRequestBody() {
        MethodWrapper method = new DefaultMethodWrapper(ListAPIGatewayTestFunction.class, "handler");
        APIGatewayRequestEvent requestEvent = coerceRequest(method, "[{\"name\":\"Spot\",\"age\":6}]");

        assertEquals("Spot", ((List<Animal>) requestEvent.getBody()).get(0).getName());
        assertEquals(6, ((List<Animal>) requestEvent.getBody()).get(0).getAge());
    }

    @Test
    public void testSingleObjectRequestBody() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        APIGatewayRequestEvent requestEvent = coerceRequest(method, "{\"name\":\"Spot\",\"age\":6}");

        assertEquals("Spot", ((Animal) requestEvent.getBody()).getName());
        assertEquals(6, ((Animal) requestEvent.getBody()).getAge());
    }

    @Test
    public void testUncheckedHandler() {
        MethodWrapper method = new DefaultMethodWrapper(UncheckedAPIGatewayTestFunction.class, "handler");
        Headers h = Headers.emptyHeaders().setHeader("H1", "h1val");

        APIGatewayRequestEvent requestEvent = coerceRequest(method, h, "{\"name\":\"Spot\",\"age\":6}");

        assertEquals("{\"name\":\"Spot\",\"age\":6}", requestEvent.getBody());
    }

    @Test
    public void testFailureToParseIsUserFriendlyError() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> coerceRequest(method, "INVALID JSON"));

        assertEquals("Failed to coerce event to user function parameter type [simple type, class com.fnproject.events.testfns.Animal]", exception.getMessage());
        assertTrue(exception.getCause().getMessage().startsWith("Unrecognized token 'INVALID':"));
    }

    @Test
    public void testCoerceForGrandChild() {
        MethodWrapper method = new DefaultMethodWrapper(GrandChildGatewayTestFunction.class, "handler");
        APIGatewayRequestEvent requestEvent = coerceRequest(method, "simple string");

        assertEquals("simple string", requestEvent.getBody());
    }

    @Test
    public void testReturnEmptyResponseWhenNotAPIGatewayClass() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayCoercionTest.class, "testMethod");

        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>().build();
        Optional<OutputEvent> outputEvent = coercion.wrapFunctionResult(responseInvocationContext, method, responseEvent);

        assertFalse(outputEvent.isPresent());
    }

    @Test
    public void testResponseHeaders() {
        Headers headers = Headers.emptyHeaders()
            .addHeader("custom-header", "customValue")
            .setHeaders(Collections.singletonMap("custom-header-2", Collections.singletonList("customValue2")));
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .headers(headers)
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("customValue",
            responseInvocationContext.getAdditionalResponseHeaders().get("Fn-Http-H-Custom-Header").get(0));
        assertEquals("customValue2",
            responseInvocationContext.getAdditionalResponseHeaders().get("Fn-Http-H-Custom-Header-2").get(0));
    }

    @Test
    public void testResponseRepeatHeaders() {
        Headers headers = Headers.emptyHeaders()
            .addHeader("repeat", "1")
            .addHeader("repeat", "2");
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .headers(headers)
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("2",
            responseInvocationContext.getAdditionalResponseHeaders().get("Fn-Http-H-Repeat").get(1));
        assertEquals("1",
            responseInvocationContext.getAdditionalResponseHeaders().get("Fn-Http-H-Repeat").get(0));
    }

    @Test
    public void testResponseContentTypeDefaultString() {
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("text/plain", outputEvent.get().getContentType().get());
    }

    @Test
    public void testResponseContentTypeDefault() {
        APIGatewayResponseEvent<Car> responseEvent = new APIGatewayResponseEvent.Builder<Car>()
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("application/json", outputEvent.get().getContentType().get());
    }

    @Test
    public void testResponseCustomContentType() {
        Headers headers = Headers.emptyHeaders()
            .addHeader("content-type", "application/octet-stream");
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .headers(headers)
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("application/octet-stream", outputEvent.get().getContentType().get());
    }

    @Test
    public void testResponseStatus() {
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .statusCode(200)
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("200", responseInvocationContext.getAdditionalResponseHeaders().get("Fn-Http-Status").get(0));
    }

    @Test
    public void testResponseStringBody() throws IOException {
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .body("string body")
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        String actual = writeToString(outputEvent.get());
        assertEquals("string body", actual);
    }

    @Test
    public void testResponseObjectBody() throws IOException {
        Car car = new Car("ford", 4);
        APIGatewayResponseEvent<Car> responseEvent = new APIGatewayResponseEvent.Builder<Car>()
            .body(car)
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        String actual = writeToString(outputEvent.get());
        assertEquals("{\"brand\":\"ford\",\"wheels\":4}", actual);
    }

    @Test
    public void testResponseListBody() throws IOException {
        Car car = new Car("ford", 4);
        APIGatewayResponseEvent<List<Car>> responseEvent = new APIGatewayResponseEvent.Builder<List<Car>>()
            .body(Collections.singletonList(car))
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(APIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        String actual = writeToString(outputEvent.get());
        assertEquals("[{\"brand\":\"ford\",\"wheels\":4}]", actual);
    }

    @Test
    public void testResponseStringNullBody() {
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(StringAPIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("text/plain", outputEvent.get().getContentType().get());
    }

    @Test
    public void testResponseUncheckedBody() {
        APIGatewayResponseEvent<String> responseEvent = new APIGatewayResponseEvent.Builder<String>()
            .build();

        DefaultMethodWrapper method = new DefaultMethodWrapper(UncheckedAPIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(responseEvent, method);

        assertTrue(outputEvent.isPresent());
        assertEquals("text/plain", outputEvent.get().getContentType().get());
    }

    @Test
    public void testNullResponse() {
        DefaultMethodWrapper method = new DefaultMethodWrapper(UncheckedAPIGatewayTestFunction.class, "handler");
        Optional<OutputEvent> outputEvent = wrap(null, method);

        assertFalse(outputEvent.isPresent());
    }

    private Optional<OutputEvent> wrap(APIGatewayResponseEvent<?> responseEvent, MethodWrapper method) {
        Headers requestHeaders = Headers.emptyHeaders();
        InputEvent inputEvent = mock(InputEvent.class);
        when(inputEvent.getHeaders()).thenReturn(requestHeaders);

        FunctionRuntimeContext frc = new FunctionRuntimeContext(method, new HashMap<>());

        responseInvocationContext = new DefaultFunctionInvocationContext(frc, inputEvent);
        return coercion.wrapFunctionResult(responseInvocationContext, method, responseEvent);
    }

    private static String writeToString(OutputEvent oe) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        oe.writeToOutput(baos);
        return baos.toString("UTF-8");
    }

    private APIGatewayRequestEvent coerceRequest(MethodWrapper method, Headers headers, String body) {
        when(requestinvocationContext.getRequestHeaders()).thenReturn(headers);
        ByteArrayInputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        return coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method).orElseThrow(RuntimeException::new);
    }

    private APIGatewayRequestEvent coerceRequest(MethodWrapper method, String body) {
        return coerceRequest(method, Headers.emptyHeaders(), body);
    }

    public String testMethod(List<Animal> ss) {
        // This method isn't actually called, it only exists to have its parameter types examined by the JacksonCoercion
        return ss.get(0).getName();
    }
}