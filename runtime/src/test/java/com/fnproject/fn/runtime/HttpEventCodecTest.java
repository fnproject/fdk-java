package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.fnproject.fn.runtime.HeaderBuilder.headerEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
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
       "Fn_Method: GET\n" +
       "Content-Length: 0\n" +
       "Fn_Call_Id: task-id2\n" +
       "Myconfig: fooconfig\n\n";

    private final Map<String, String> emptyConfig = new HashMap<>();

    private  Map<String, String> env() {
        HashMap<String, String> env = new HashMap<>();
        env.put("FN_APP_NAME", "testapp");
        env.put("FN_PATH", "/test");
        return env;
    }

    @Test
    public void testParsingSimpleHttpRequestWithFnHeadersAndBody() {
        ByteArrayInputStream bis = new ByteArrayInputStream(postReq.getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), bis, bos);

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
        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), bis, bos);

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
            HttpEventCodec httpEventCodec = new HttpEventCodec(env(), asStream("NOT_HTTP " + getReq), nullOut);

            httpEventCodec.readEvent();
            fail();
        } catch (FunctionInputHandlingException e) {
            assertThat(e).hasMessageContaining("Failed to read HTTP content from input");
        }
    }



    @Test
    public void shouldSerializeSimpleSuccessfulEvent() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), nullIn, bos);
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(), OutputEvent.Status.Success, "text/plain");

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        assertThat(statusLine(httpResponse)).isEqualTo("HTTP/1.1 200 Success");
        assertThat(headers(httpResponse)).containsOnly(entry("content-type", "text/plain"), entry("content-length", "5"));
        assertThat(body(httpResponse)).isEqualTo("Hello");

    }

    private static String statusLine(String httpResponse) {
        return httpResponse.split("\\\r\\\n", 2)[0];
    }

    private static Map<String, String> headers(String httpResponse) {
        Map<String, String> hs = new HashMap<>();
        boolean firstLine = true;
        for (String line : httpResponse.split("\\\r\\\n")) {
            if (line.equals("")) {
                break;
            }
            if (firstLine) {
                firstLine = false;
                continue;
            }
            String[] parts = line.split(": *", 2);
            hs.put(parts[0].toLowerCase(), parts[1]);
        }
        return hs;
    }

    private static String body(String httpResponse) {
        return httpResponse.split("\\\r\\\n\\\r\\\n", 2)[1];
    }

    @Test
    public void shouldSerializeSuccessfulEventWithHeaders() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), nullIn, bos);
        Map<String, String> hs = new HashMap<>();
        hs.put("foo", "bar");
        hs.put("Content-Type", "application/octet-stream"); // ignored
        hs.put("Content-length", "99");  // ignored
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(), OutputEvent.Status.Success, "text/plain", Headers.fromMap(hs));

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        assertThat(statusLine(httpResponse)).isEqualTo("HTTP/1.1 200 Success");
        assertThat(headers(httpResponse)).containsOnly(entry("foo", "bar"),
           entry("content-type", "text/plain"),
           entry("content-length", "5"));
        assertThat(body(httpResponse)).isEqualTo("Hello");
    }


    @Test
    public void shouldSerializeSimpleFailedEvent() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), nullIn, bos);
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(), OutputEvent.Status.FunctionError, "text/plain");

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        assertThat(statusLine(httpResponse)).isEqualTo("HTTP/1.1 502 FunctionError");
        assertThat(headers(httpResponse)).containsOnly(entry("content-type", "text/plain"),
           entry("content-length", "5"));
        assertThat(body(httpResponse)).isEqualTo("Hello");
    }


    @Test
    public void shouldSerializeFailedEventWithHeaders() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        HttpEventCodec httpEventCodec = new HttpEventCodec(env(), nullIn, bos);
        Map<String, String> hs = new HashMap<>();
        hs.put("foo", "bar");
        hs.put("Content-Type", "application/octet-stream"); // ignored
        hs.put("Content-length", "99");  // ignored
        OutputEvent outEvent = OutputEvent.fromBytes("Hello".getBytes(), OutputEvent.Status.FunctionError, "text/plain", Headers.fromMap(hs));

        httpEventCodec.writeEvent(outEvent);
        String httpResponse = new String(bos.toByteArray());

        assertThat(statusLine(httpResponse)).isEqualTo("HTTP/1.1 502 FunctionError");
        assertThat(headers(httpResponse)).containsOnly(entry("foo", "bar"),
           entry("content-type", "text/plain"),
           entry("content-length", "5"));
        assertThat(body(httpResponse)).isEqualTo("Hello");
    }


    private InputStream asStream(String sin) {
        return new ByteArrayInputStream(sin.getBytes());
    }

    private void isExpectedGetEvent(InputEvent getEvent) {
        //assertThat(getEvent.getAppName()).isEqualTo("testapp");
//        assertThat(getEvent.getMethod()).isEqualTo("GET");
//        assertThat(getEvent.getRoute()).isEqualTo("/test");

        assertThat(getEvent.getHeaders().asMap())
           .contains(headerEntry("Accept-Encoding", "gzip"),
              headerEntry("User-Agent", "useragent"));


        getEvent.consumeBody((is) -> assertThat(is).hasSameContentAs(asStream("")));
    }


    private void isExpectedPostEvent(InputEvent postEvent) {
        //assertThat(postEvent.getAppName()).isEqualTo("testapp");
//        assertThat(postEvent.getMethod()).isEqualTo("POST");
//        assertThat(postEvent.getRoute()).isEqualTo("/test");
        assertThat(postEvent.getHeaders().asMap().size()).isEqualTo(11);
        assertThat(postEvent.getHeaders().asMap())
           .contains(headerEntry("Accept", "text/html, text/plain;q=0.9"),
              headerEntry("Accept-Encoding", "gzip"),
              headerEntry("User-Agent", "useragent"),
              headerEntry("Content-Type", "text/plain"));

        postEvent.consumeBody((is) -> assertThat(is).hasSameContentAs(asStream("Hello World")));
    }
//

}
