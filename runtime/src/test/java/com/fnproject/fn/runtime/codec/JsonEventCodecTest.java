package com.fnproject.fn.runtime.codec;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonEventCodecTest {

    private String jsonInputEvent =
            "{\n"+
            "  \"call_id\": \"123\",\n"+
            "  \"content_type\": \"application/json\",\n"+
            "  \"deadline\":\"2018-01-30T16:52:39.786Z\",\n"+
            "  \"body\": \"{\\\"some\\\":\\\"input\\\"}\",\n"+
            "  \"protocol\": {\n"+
            "    \"type\": \"http\",\n"+
            "    \"method\": \"POST\",\n"+
            "    \"request_url\": \"http://localhost:8080/r/myapp/myfunc?q=hi\",\n"+
            "    \"headers\": {\n"+
            "      \"Content-Type\": [\"application/json\"],\n"+
            "      \"Other-Header\": [\"something\", \"something else\"]\n"+
            "    }\n"+
            "  }\n"+
            "}";


    private final Map<String, String> env() {
        HashMap<String, String> env = new HashMap<>();
        env.put("FN_APP_NAME", "testapp");
        env.put("FN_PATH", "/test");
        return env;
    }

    private JsonEventCodec createJsonEventCodecForInput(String s) {
        return new JsonEventCodec(env(), new ByteArrayInputStream(s.getBytes()), new ByteArrayOutputStream());
    }


    @Test(expected = FunctionInputHandlingException.class)
    public void returnsOptionalEmptyOnEmptyInput(){
        createJsonEventCodecForInput("").readEvent();
    }

    @Test(expected = FunctionInputHandlingException.class)
    public void returnsOptionalEmptyOnNonJsonInput(){
        createJsonEventCodecForInput("This isn't JSON").readEvent();
    }

    @Test(expected = FunctionInputHandlingException.class)
    public void returnsOptionalEmptyOnNonSchemaJsonInput(){
        createJsonEventCodecForInput("{}").readEvent();
    }

    @Test
    public void parsesExampleJsonFromDocs(){
        // Docs: https://github.com/fnproject/fn/blob/master/docs/developers/function-format.md

        JsonEventCodec jsonCodec = createJsonEventCodecForInput(jsonInputEvent);
        InputEvent event = jsonCodec.readEvent().get();

        assertThat(event.getAppName()).isEqualTo("testapp");
        assertThat(event.getMethod()).isEqualTo("POST");
        assertThat(event.getRequestUrl()).isEqualTo("http://localhost:8080/r/myapp/myfunc?q=hi");
        assertThat(event.getRoute()).isEqualTo("/test");

        Headers headers = event.getHeaders();
        assertThat(headers.getAll().size()).isEqualTo(2);
        assertThat(headers.get("content-type").get()).isEqualTo("application/json");
        assertThat(headers.get("other-header").get()).isEqualTo("something,something else");

        QueryParameters queryParameters = event.getQueryParameters();
        assertThat(queryParameters.getAll().size()).isEqualTo(1);
        assertThat(queryParameters.get("q").get()).isEqualTo("hi");
    }



}
