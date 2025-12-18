package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.APIGatewayCoercion.OM_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.notification.NotificationObjectTestFunction;
import com.fnproject.events.testfns.notification.NotificationStringTestFunction;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.DefaultMethodWrapper;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import org.junit.Before;
import org.junit.Test;


public class NotificationCoercionTest {
    private NotificationCoercion coercion;
    private InvocationContext requestinvocationContext;

    @Before
    public void setUp() {
        coercion = NotificationCoercion.instance();
        requestinvocationContext = mock(InvocationContext.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        ObjectMapper mapper = new ObjectMapper();

        when(runtimeContext.getAttribute(OM_KEY, ObjectMapper.class)).thenReturn(Optional.of(mapper));
        when(requestinvocationContext.getRuntimeContext()).thenReturn(runtimeContext);
    }

    @Test
    public void testReturnEmptyWhenNotNotificationClass() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayCoercionTest.class, "testMethod");

        Headers headers = Headers.emptyHeaders();

        when(requestinvocationContext.getRequestHeaders()).thenReturn(headers);
        ByteArrayInputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        Optional<?> batch = coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method);

        assertFalse(batch.isPresent());
    }

    @Test
    public void testNotificationObjectInput() {
        MethodWrapper method = new DefaultMethodWrapper(NotificationObjectTestFunction.class, "handler");

        NotificationMessage<Animal> event = coerceRequest(method, "{\"name\":\"foo\",\"age\":3}");

        assertEquals(3, event.getContent().getAge());
        assertEquals("foo", event.getContent().getName());
    }

    @Test
    public void testNotificationStringInput() {
        MethodWrapper method = new DefaultMethodWrapper(NotificationStringTestFunction.class, "handler");
        NotificationMessage<String> event = coerceRequest(method, "a plain string");

        assertEquals("a plain string", event.getContent());
    }

    @Test
    public void testNotificationStringInputEmpty() {
        MethodWrapper method = new DefaultMethodWrapper(NotificationStringTestFunction.class, "handler");

        NotificationMessage<String> event = coerceRequest(method, "");

        assertEquals("", event.getContent());
    }

    @Test
    public void testHeaders() {
        MethodWrapper method = new DefaultMethodWrapper(NotificationObjectTestFunction.class, "handler");
        when(requestinvocationContext.getRequestHeaders()).thenReturn(Headers.emptyHeaders().addHeader("foo", "bar"));
        NotificationMessage<String > event = coerceRequest(method, "{\"name\": \"cat\",\"age\":2}");

        assertEquals("bar", event.getHeaders().get("foo").get());
    }

    @Test
    public void testEmptyHeaders() {
        MethodWrapper method = new DefaultMethodWrapper(NotificationObjectTestFunction.class, "handler");
        when(requestinvocationContext.getRequestHeaders()).thenReturn(Headers.emptyHeaders());
        NotificationMessage<String > event = coerceRequest(method, "{\"name\": \"cat\",\"age\":2}");

        assertEquals(0, event.getHeaders().asMap().size());
    }

    private NotificationMessage coerceRequest(MethodWrapper method, String body) {
        ByteArrayInputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        return coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method).orElseThrow(RuntimeException::new);
    }
}