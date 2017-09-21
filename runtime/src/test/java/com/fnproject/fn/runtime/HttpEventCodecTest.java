package com.fnproject.fn.runtime;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fnproject.fn.runtime.HeaderBuilder.headerEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HttpEventCodecTest {
    private final OutputStream nullOut = new NullOutputStream();
    private final InputStream nullIn = new NullInputStream(0);

    private final String postReq = "GET /test HTTP/1.1\n" +
            "Accept-Encoding: gzip\n" +
            "User-Agent: useragent\n" +
            "Accept: text/html, text/plain;q=0.9\n" +
            "Fn_Request_url: http//localhost:8080/r/testapp/test\n" +
            "Fn_Path: /test\n" +
            "Fn_Method: POST\n" +
            "Content-Length: 11\n" +
            "Fn_App_name: testapp\n" +
            "Fn_Call_id: task-id\n" +
            "Myconfig: fooconfig\n" +
            "Content-Type: text/plain\n\n" +
            "Hello World";


    private final String getReq = "GET /test HTTP/1.1\n" +
            "Accept-Encoding: gzip\n" +
            "User-Agent: useragent\n" +
            "Fn_Request_url: http//localhost:8080/r/testapp/test\n" +
            "Fn_Path: /test2\n" +
            "Fn_Method: GET\n" +
            "Content-Length: 0\n" +
            "Fn_App_name: testapp\n" +
            "Fn_Call_Id: task-id2\n" +
            "Myconfig: fooconfig\n\n";

    private final Map<String, String> emptyConfig = new HashMap<>();

    @Test
    public void testParsingSimpleHttpRequestWithFnHeadersAndBody() {
        ByteArrayInputStream bis = new ByteArrayInputStream(postReq.getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HttpEventCodec httpEventCodec = new HttpEventCodec(bis, bos);

        InputEvent event = httpEventCodec.readEvent().get();
        isExpectedPostEvent(event);
    }

    @Test
    public void shouldReadMultipleRequestsOnSameStream() {
        byte req1[] = postReq.getBytes();
        byte req2[] = getReq.getBytes();
        byte req3[] = postReq.getBytes();

        byte input[] = new byte[req1.length + req2.length + req3.length];

        System.arraycopy(req1, 0, input, 0, req1.length);
        System.arraycopy(req2, 0, input, req1.length, req2.length);
        System.arraycopy(req3, 0, input, req1.length + req2.length, req3.length);

        ByteArrayInputStream bis = new ByteArrayInputStream(input);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HttpEventCodec httpEventCodec = new HttpEventCodec(bis, bos);

        InputEvent postEvent = httpEventCodec.readEvent().get();
        isExpectedPostEvent(postEvent);

        InputEvent getEvent = httpEventCodec.readEvent().get();
        isExpectedGetEvent(getEvent);

        InputEvent postEvent2 = httpEventCodec.readEvent().get();
        isExpectedPostEvent(postEvent2);


    }

    @Test
    public void shouldRejectInvalidHttpRequest() {
        try {
            HttpEventCodec httpEventCodec = new HttpEventCodec(asStream("NOT_HTTP " + getReq), nullOut);

            httpEventCodec.readEvent();
            fail();
        } catch (FunctionInputHandlingException e) {
            assertThat(e).hasMessageContaining("Failed to read HTTP content from input");
        }
    }


    @Test
    public void shouldRejectMissingHttpHeaders() {
        Map<String, String> requiredHeaders = new HashMap<>();

        requiredHeaders.put("fn_request_url", "request_url");
        requiredHeaders.put("fn_path", "/route");
        requiredHeaders.put("fn_method", "GET");
        requiredHeaders.put("fn_app_name", "app_name");

        for (String key : requiredHeaders.keySet()) {
            Map<String, String> newMap = new HashMap<>(requiredHeaders);
            newMap.remove(key);
            String req = "GET / HTTP/1.1\n" + newMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n")) + "\n\n";

            try {
                HttpEventCodec httpEventCodec = new HttpEventCodec(asStream(req), nullOut);
                httpEventCodec.readEvent();
                fail("Should fail with header missing:" + key);
            } catch (FunctionInputHandlingException e) {
                assertThat(e).hasMessageMatching("Incoming HTTP frame is missing required header: " + key);
            }
        }

    }

    @Test
    public void shouldSerializeSimpleSuccessfulEvent() throws Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(nullIn,bos);
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(),true,"text/plain");

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        // TODO: this test is brittle.
        assertThat(httpResponse).isEqualTo("HTTP/1.1 200 INVOKED\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-length: 5\r\n" +
                "\r\n" +
                "Hello");

    }


    @Test
    public void shouldSerializeSimpleFailedEvent() throws Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(nullIn,bos);
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(),false,"text/plain");

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        // TODO: this test is brittle.
        assertThat(httpResponse).isEqualTo("HTTP/1.1 500 INVOKE FAILED\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-length: 5\r\n" +
                "\r\n" +
                "Hello");

    }


    private InputStream asStream(String sin) {
        return new ByteArrayInputStream(sin.getBytes());
    }

    private void isExpectedGetEvent(InputEvent getEvent) {
        assertThat(getEvent.getAppName()).isEqualTo("testapp");
        assertThat(getEvent.getMethod()).isEqualTo("GET");
        assertThat(getEvent.getRoute()).isEqualTo("/test2");

        assertThat(getEvent.getHeaders().getAll())
                .contains(headerEntry("Accept-Encoding", "gzip"),
                        headerEntry("User-Agent", "useragent"));


        getEvent.consumeBody((is) -> assertThat(is).hasSameContentAs(asStream("")));
    }

    private void isExpectedPostEvent(InputEvent postEvent) {
        assertThat(postEvent.getAppName()).isEqualTo("testapp");
        assertThat(postEvent.getMethod()).isEqualTo("POST");
        assertThat(postEvent.getRoute()).isEqualTo("/test");
        assertThat(postEvent.getHeaders().getAll().size()).isEqualTo(11);
        assertThat(postEvent.getHeaders().getAll())
                .contains(headerEntry("Accept", "text/html, text/plain;q=0.9"),
                          headerEntry("Accept-Encoding", "gzip"),
                          headerEntry("User-Agent", "useragent"),
                          headerEntry("Content-Type", "text/plain"));

        postEvent.consumeBody((is) -> assertThat(is).hasSameContentAs(asStream("Hello World")));
    }


}
