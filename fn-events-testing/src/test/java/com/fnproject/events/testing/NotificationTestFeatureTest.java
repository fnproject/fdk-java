package com.fnproject.events.testing;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.testing.FnTestingRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

public class NotificationTestFeatureTest {

    @Rule
    public final FnTestingRule fn = FnTestingRule.createDefault();

    NotificationTestFeature feature = NotificationTestFeature.createDefault(fn);

    @Test
    public void testObjectBody() throws Exception {
        Animal animal = new Animal("foo", 3);
        NotificationMessage<Animal> req = new NotificationMessage<>(animal, Headers.emptyHeaders());
        NotificationTestFeature.NotificationRequestEventBuilder builder = (NotificationTestFeature.NotificationRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals(
            "{\"name\":\"foo\",\"age\":3}",
            body);
    }

    @Test
    public void testHeadersEmptyList() throws Exception {
        NotificationMessage<String> req = new NotificationMessage<>("", Headers.emptyHeaders());
        NotificationTestFeature.NotificationRequestEventBuilder builder = (NotificationTestFeature.NotificationRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals(Headers.emptyHeaders(), inputEvent.getHeaders());
    }

    @Test
    public void testHeadersList() throws Exception {
        Headers headers = Headers.emptyHeaders().addHeader("foo", "bar");
        NotificationMessage<String> req = new NotificationMessage<>("", headers);
        NotificationTestFeature.NotificationRequestEventBuilder builder = (NotificationTestFeature.NotificationRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals("bar", inputEvent.getHeaders().get("foo").get());
    }
}