package com.fnproject.events.mapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.testfns.Animal;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl;
import org.junit.Before;
import org.junit.Test;

public class APIGatewayRequestEventMapperTest {

    private APIGatewayRequestEventMapper mapper;

    @Before
    public void setUp() {
        mapper = new APIGatewayRequestEventMapper();
    }

    @Test
    public void testQueryParameter() {
        Map<String, List<String>> params = new HashMap<>();
        List<String> paramValues = new ArrayList<>();
        paramValues.add("value");
        paramValues.add("value2");
        params.put("keyTest", paramValues);
        QueryParametersImpl qp = new QueryParametersImpl(params);

        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        when(httpGatewayContextMock.getQueryParameters()).thenReturn(qp);

        APIGatewayRequestEvent<String> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, "");
        QueryParameters queryParameters = event.getQueryParameters();

        assertEquals("value", queryParameters.getValues("keyTest").get(0));
        assertEquals("value2", queryParameters.getValues("keyTest").get(1));
    }

    @Test
    public void testGetBody() {
        String payload = "value";

        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        APIGatewayRequestEvent<String> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, payload);
        String body = event.getBody();

        assertEquals("value", body);
    }

    @Test
    public void testGetBodyObject() {
        Animal request = new Animal("value", 1);
        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        APIGatewayRequestEvent<Animal> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, request);
        Animal body = event.getBody();

        assertEquals(request, body);
    }

    @Test
    public void testGetNullBodyObject() {
        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        APIGatewayRequestEvent<Animal> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, null);
        Animal body = event.getBody();

        assertNull(body);
    }

    @Test
    public void testGetMethod() {
        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        when(httpGatewayContextMock.getMethod()).thenReturn("GET");
        APIGatewayRequestEvent<String> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, "");
        String method = event.getMethod();

        assertEquals("GET", method);
    }

    @Test
    public void testGetHeaders() {
        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);

        Headers headers = Headers.emptyHeaders().addHeader("key1", "value1");
        when(httpGatewayContextMock.getHeaders()).thenReturn(headers);

        APIGatewayRequestEvent<String> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, "");
        Headers eventHeaders = event.getHeaders();

        assertEquals("value1", eventHeaders.get("key1").get());
    }

    @Test
    public void testGetRepeatedHeaders() {
        HTTPGatewayContext httpGatewayContextMock = mock(HTTPGatewayContext.class);
        Headers headers = Headers.emptyHeaders().addHeader("repeat", "1", "2");
        when(httpGatewayContextMock.getHeaders()).thenReturn(headers);

        APIGatewayRequestEvent<String> event = mapper.toApiGatewayRequestEvent(httpGatewayContextMock, "");
        Headers eventHeaders = event.getHeaders();

        assertEquals("1", eventHeaders.getAllValues("repeat").get(0));
        assertEquals("2", eventHeaders.getAllValues("repeat").get(1));
    }
}