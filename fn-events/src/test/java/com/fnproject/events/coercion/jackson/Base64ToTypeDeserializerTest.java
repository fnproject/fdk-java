package com.fnproject.events.coercion.jackson;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fnproject.events.input.sch.StreamingData;
import com.fnproject.events.testfns.Animal;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class Base64ToTypeDeserializerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    // Helpers
    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String animalJson(String name, int age) {
        return "{\"name\":\"" + name + "\",\"age\":" + age + "}";
    }

    @Test
    public void testDecodeBase64JsonIntoPojo() throws Exception {
        String valueB64 = b64(animalJson("Felix", 2));
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\":\"" + valueB64 + "\", \"offset\":\"o\" }";

        StreamingData<Animal> sd = mapper.readValue(json, new TypeReference<StreamingData<Animal>>() {
        });
        assertThat(sd.getValue(), instanceOf(Animal.class));
        assertEquals("Felix", sd.getValue().getName());
        assertEquals(2, sd.getValue().getAge());
    }

    @Test
    public void testDecodeBase64TextIntoString() throws Exception {
        String valueB64 = b64("hello world");
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\":\"" + valueB64 + "\", \"offset\":\"o\" }";

        StreamingData<String> sd = mapper.readValue(json, new TypeReference<StreamingData<String>>() {
        });
        assertEquals("hello world", sd.getValue());
    }

    @Test
    public void testPassThroughNonBase64StringForStringTarget() throws Exception {
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\":\"not-base64$\", \"offset\":\"o\" }";

        StreamingData<String> sd = mapper.readValue(json, new TypeReference<StreamingData<String>>() {
        });
        assertEquals("not-base64$", sd.getValue());
    }

    @Test
    public void testInterpretPlainJsonStringWhenNotBase64() throws Exception {
        // value is a string that contains JSON; not base64
        String inner = animalJson("Buddy", 1);
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\":\"" + inner.replace("\"", "\\\"") + "\", \"offset\":\"o\" }";

        StreamingData<Animal> sd = mapper.readValue(json, new TypeReference<StreamingData<Animal>>() {
        });
        assertThat(sd.getValue(), instanceOf(Animal.class));
        assertEquals("Buddy", sd.getValue().getName());
        assertEquals(1, sd.getValue().getAge());
    }

    @Test
    public void testNonStringValueDelegatesNormally() throws Exception {
        // The value is already a JSON object (not a string). The deserializer should delegate to normal binding.
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\": " + animalJson("Milo", 3) + ", \"offset\":\"o\" }";

        StreamingData<Animal> sd = mapper.readValue(json, new TypeReference<StreamingData<Animal>>() {
        });
        assertThat(sd.getValue(), instanceOf(Animal.class));
        assertEquals("Milo", sd.getValue().getName());
        assertEquals(3, sd.getValue().getAge());
    }

    @Test
    public void testNullValueReturnsNull() throws Exception {
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\": null, \"offset\":\"o\" }";

        StreamingData<Animal> sd = mapper.readValue(json, new TypeReference<StreamingData<Animal>>() {
        });
        assertNull(sd.getValue());
    }

    @Test
    public void testResolveTypeFromEnclosingGenericInCollection() throws Exception {
        String v1 = b64(animalJson("Felix", 1));
        String v2 = b64(animalJson("Buddy", 2));
        String json = "[ " +
            "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k1\", \"value\":\"" + v1 + "\", \"offset\":\"o1\" }," +
            "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k2\", \"value\":\"" + v2 + "\", \"offset\":\"o2\" }" +
            "]";

        List<StreamingData<Animal>> list = mapper.readValue(
            json, new TypeReference<List<StreamingData<Animal>>>() {
            });
        assertEquals(2, list.size());
        assertThat(list.get(0).getValue(), instanceOf(Animal.class));
        assertEquals("Felix", list.get(0).getValue().getName());
        assertThat(list.get(1).getValue(), instanceOf(Animal.class));
        assertEquals("Buddy", list.get(1).getValue().getName());
    }

    @Test
    public void testInvalidNonJsonForNonStringTargetYieldsMismatch() {
        // Not base64; not JSON; not coercible to Animal -> should surface a MismatchedInputException
        String json = "{ \"stream\":\"s\", \"partition\":\"p\", \"key\":\"k\", \"value\":\"not-json-or-base64\", \"offset\":\"o\" }";

        assertThrows(MismatchedInputException.class, () -> mapper.readValue(json, new TypeReference<StreamingData<Animal>>() {}));
    }

} 