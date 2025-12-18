package com.fnproject.events.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import java.util.List;
import com.fnproject.events.testfns.Animal;
import com.fnproject.fn.api.Headers;
import org.junit.Test;

public class APIGatewayResponseEventTest {

    @Test
    public void testGetBody() {
        String body = "body";
        APIGatewayResponseEvent<String> event = new APIGatewayResponseEvent.Builder<String>().body(body).build();
        assertEquals("body", event.getBody());
    }

    @Test
    public void testGetBodyAsObject() {
        Animal response = new Animal("body", 1);
        APIGatewayResponseEvent<Animal> event = new APIGatewayResponseEvent.Builder<Animal>().body(response).build();
        assertEquals(response, event.getBody());
    }

    @Test
    public void testGetBodyAsList() {
        Animal response = new Animal("body", 1);
        List<Animal> list = Collections.singletonList(response);
        APIGatewayResponseEvent<List<Animal>> event = new APIGatewayResponseEvent.Builder<List<Animal>>().body(list).build();
        assertEquals(list, event.getBody());
    }

    @Test
    public void testGetNullBody() {
        APIGatewayResponseEvent<List<Animal>> event = new APIGatewayResponseEvent.Builder<List<Animal>>().body(null).build();
        assertNull(event.getBody());
    }

    @Test
    public void testGetStatus() {
        APIGatewayResponseEvent<String> event = new APIGatewayResponseEvent.Builder<String>().statusCode(201).build();
        assertEquals(Integer.valueOf(201), event.getStatus());
    }

    @Test
    public void testGetHeaders() {
        Headers headers = Headers.emptyHeaders().addHeader("foo","bar");
        APIGatewayResponseEvent<String> event = new APIGatewayResponseEvent.Builder<String>().headers(headers).build();
        assertEquals(headers, event.getHeaders());
    }

    @Test
    public void testGetRepeatedHeaders() {
        Headers headers = Headers.emptyHeaders().addHeader("repeated", "foo").addHeader("repeated", "bar");
        APIGatewayResponseEvent<String> event = new APIGatewayResponseEvent.Builder<String>()
            .headers(headers)
            .build();
        assertEquals("foo", event.getHeaders().getAllValues("repeated").get(0));
        assertEquals("bar", event.getHeaders().getAllValues("repeated").get(1));
    }
}