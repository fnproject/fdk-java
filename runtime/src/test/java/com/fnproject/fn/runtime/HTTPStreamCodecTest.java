package com.fnproject.fn.runtime;


import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import org.apache.commons.io.IOUtils;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class HTTPStreamCodecTest {
    private static final String fileName;

    private static final String FN_LISTENER;

    private static final Map<String, String> defaultEnv;

    private HTTPStreamCodec codec;

    private static File createSocketFile() {
        File f = null;
        try {

            f = File.createTempFile("socket", ".sock");
            f.delete();
            f.deleteOnExit();
        } catch (IOException e) {
        }

        return f;
    }

    static {
        System.setProperty("com.fnproject.java.native.libdir", new File("src/main/c/").getAbsolutePath());

        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        fileName = createSocketFile().getAbsolutePath();
        FN_LISTENER = "unix:" + fileName;
        httpClient = new HttpClient(new HttpClientTransportOverUnixSockets(fileName), null);


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
    public void cleanup() throws Exception {
        File listener = new File(fileName);
        listener.delete();
        if (codec != null) {
            codec.close();
            codec = null;
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
        assertThat(evt.getDeadline().toEpochMilli()).isEqualTo(1033552800992L);
        assertThat(evt.getHeaders()).isEqualTo(Headers.emptyHeaders().addHeader("Fn-Call-Id", "callID").addHeader("Fn-Deadline", "2002-10-02T10:00:00.992Z").addHeader("Custom-header", "v1", "v2").addHeader("Content-Type", "text/plain"));


    }


    @Test
    public void shouldHandleMultipleRequests() throws Exception {
        AtomicReference<String> lastInput = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger(0);

        startCodec(defaultEnv, (in) -> {
            lastInput.set(in.consumeBody((is) -> {
                try {
                    return IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
            return OutputEvent.fromBytes(String.format("%d", count.getAndIncrement()).getBytes(), OutputEvent.Status.Success, "text/plain", Headers.emptyHeaders());
        });

        for (int i = 0; i < 5; i++) {
            ContentResponse resp = httpClient.newRequest("http://localhost/call")
              .method("POST")
              .header("Fn-Call-Id", "callID")
              .header("Fn-Deadline", "2002-10-02T10:00:00.992Z")
              .header("Custom-header", "v1")
              .header("Custom-header", "v2")
              .header("Content-Type", "text/plain")
              .content(new StringContentProvider(String.format("%d", i))).send();

            assertThat(resp.getStatus()).isEqualTo(200);
            assertThat(resp.getContent()).isEqualTo(String.format("%d", i).getBytes());
            assertThat(lastInput).isNotNull();
            assertThat(lastInput.get()).isEqualTo(String.format("%d", i));
        }

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

    @Test
    public void shouldStripHopToHopHeadersFromFunctionInput() throws Exception {

        for (String header[] : new String[][]{
          {"Content-Length", "0"},
          {"Transfer-encoding", "chunked"},
          {"Connection", "close"},
        }) {
            CompletableFuture<InputEvent> lastEvent = new CompletableFuture<>();

            startCodec(defaultEnv, (in) -> {
                lastEvent.complete(in);
                return OutputEvent.fromBytes("hello".getBytes(), OutputEvent.Status.Success, "text/plain", Headers.emptyHeaders().addHeader(header[0], header[1]));
            });

            ContentResponse resp = defaultRequest().send();

            assertThat(resp.getHeaders().get(header[0])).isNull();

            cleanup();
        }
    }

    @Test
    public void socketShouldHaveCorrectPermissions() throws Exception {
        startCodec(defaultEnv, (in) -> OutputEvent.fromBytes("hello".getBytes(), OutputEvent.Status.Success, "text/plain", Headers.emptyHeaders()));
        File listener = new File(fileName);

        assertThat(Files.getPosixFilePermissions(listener.toPath(),LinkOption.NOFOLLOW_LINKS)).isEqualTo(PosixFilePermissions.fromString("rwxrwxrwx"));

        cleanup();
    }
}
