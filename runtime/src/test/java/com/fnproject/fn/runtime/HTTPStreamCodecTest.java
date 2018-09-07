package com.fnproject.fn.runtime;


import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.unixsocket.client.HttpClientTransportOverUnixSockets;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class HTTPStreamCodecTest {
    private static final String FN_LISTENER = "/tmp/fn.sock";

    private static final Map<String, String> defaultEnv;

    private HTTPStreamCodec codec;

    static {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        httpClient = new HttpClient(new HttpClientTransportOverUnixSockets(FN_LISTENER), null);

        Map<String, String> env = new HashMap<>();
        env.put("FN_APP_NAME", "myapp");
        env.put("FN_PATH", "mypath");
        env.put("FN_LISTENER", FN_LISTENER);

        defaultEnv = Collections.unmodifiableMap(env);
    }

    private static HttpClient httpClient;

    private Request defaultRequest() {
        return httpClient.newRequest("http://localhost/call")
           .method("POST")
           .header("Fn-Call-Id", "callID")
           .header("Fn-Deadline", "2002-10-02T10:00:00.992Z")
           .header("Custom-header", "v1")
           .header("Custom-header", "v2")
           .header("Content-Type", "text/plain")
           .content(new StringContentProvider("hello "));

    }


    @BeforeClass
    public static void setup() throws Exception {
        httpClient.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        httpClient.stop();
    }

    @After
    public void cleanup() {
        File listener = new File(FN_LISTENER);
        listener.delete();
        if (codec != null) {
            codec.close();
        }

    }


    public void startCodec(Map<String, String> env, EventCodec.Handler h) {
        codec = new HTTPStreamCodec(defaultEnv);
        Thread t = new Thread(() -> codec.runCodec(h));
        t.start();
    }

    @Test
    public void shouldAcceptDataOnHttp() throws Exception {
        CompletableFuture<InputEvent> lastEvent = new CompletableFuture<>();

        startCodec(defaultEnv, (in) -> {
            lastEvent.complete(in);
            return OutputEvent.fromBytes("hello".getBytes(), OutputEvent.Status.Success, "text/plain", Headers.emptyHeaders().addHeader("x-test", "bar"));
        });

        ContentResponse resp = httpClient.newRequest("http://localhost/call")
           .method("POST")
           .header("Fn-Call-Id", "callID")
           .header("Fn-Deadline", "2002-10-02T10:00:00.992Z")
           .header("Custom-header", "v1")
           .header("Custom-header", "v2")
           .header("Content-Type", "text/plain")
           .content(new StringContentProvider("hello ")).send();

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(resp.getContent()).isEqualTo("hello".getBytes());
        assertThat(resp.getHeaders().get("x-test")).isEqualTo("bar");

        InputEvent evt = lastEvent.get(1, TimeUnit.MILLISECONDS);
        assertThat(evt.getCallID()).isEqualTo("callID");
        assertThat(evt.getDeadline().getTime()).isEqualTo(1033552800992L);
        assertThat(evt.getHeaders()).isEqualTo(Headers.emptyHeaders().addHeader("Fn-Call-Id", "callID").addHeader("Fn-Deadline", "2002-10-02T10:00:00.992Z").addHeader("Custom-header", "v1", "v2").addHeader("Content-Type", "text/plain"));


    }

    @Test
    public void shouldConvertStatusResponses() throws Exception {

        for (OutputEvent.Status s : OutputEvent.Status.values()) {
            CompletableFuture<InputEvent> lastEvent = new CompletableFuture<>();

            startCodec(defaultEnv, (in) -> {
                lastEvent.complete(in);
                return OutputEvent.fromBytes("hello".getBytes(), s, "text/plain", Headers.emptyHeaders());
            });

            ContentResponse resp = defaultRequest().send();

            assertThat(resp.getStatus()).isEqualTo(s.getCode());

            cleanup();
        }

    }
}
