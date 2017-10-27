package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.fnproject.fn.runtime.HeaderBuilder.headerEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DefaultEventCodecTest {

    private final Map<String, String> emptyConfig = new HashMap<>();
    private InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    public void shouldExtractBasicEvent() {
        Map<String, String> env = new HashMap<>();
        env.put("FN_FORMAT", "default");
        env.put("FN_METHOD", "GET");
        env.put("FN_APP_NAME", "testapp");
        env.put("FN_PATH", "/route");
        env.put("FN_CALL_ID", "call-id");
        env.put("FN_REQUEST_URL", "http://test.com/fn/tryInvoke");

        env.put("FN_HEADER_CONTENT_TYPE", "text/plain");
        env.put("FN_HEADER_ACCEPT", "text/html, text/plain;q=0.9");
        env.put("FN_HEADER_ACCEPT_ENCODING", "gzip");
        env.put("FN_HEADER_USER_AGENT", "userAgent");

        Map<String, String> config = new HashMap<>();
        config.put("configparam", "configval");
        config.put("CONFIGPARAM", "CONFIGVAL");

        DefaultEventCodec codec = new DefaultEventCodec(env, asStream("input"), new NullOutputStream());
        InputEvent evt = codec.readEvent().get();
        assertThat(evt.getMethod()).isEqualTo("GET");
        assertThat(evt.getAppName()).isEqualTo("testapp");
        assertThat(evt.getPath()).isEqualTo("/route");
        assertThat(evt.getCallId()).isEqualTo("call-id");
        assertThat(evt.getRequestUrl()).isEqualTo("http://test.com/fn/tryInvoke");


        assertThat(evt.getHeaders().getAll().size()).isEqualTo(4);
        assertThat(evt.getHeaders().getAll()).contains(
                headerEntry("CONTENT_TYPE", "text/plain"),
                headerEntry("ACCEPT_ENCODING", "gzip"),
                headerEntry("ACCEPT", "text/html, text/plain;q=0.9"),
                headerEntry("USER_AGENT", "userAgent"));

        evt.consumeBody((body) -> assertThat(body).hasSameContentAs(asStream("input")));

        assertThat(codec.shouldContinue()).isFalse();
    }



    @Test
    public void shouldRejectMissingEnv() {
        Map<String, String> requiredEnv = new HashMap<>();

        requiredEnv.put("FN_PATH", "/route");
        requiredEnv.put("FN_METHOD", "GET");
        requiredEnv.put("FN_APP_NAME", "app_name");
        requiredEnv.put("FN_REQUEST_URL", "http://test.com/fn/tryInvoke");
        requiredEnv.put("FN_CALL_ID", "call-id");

        for (String key : requiredEnv.keySet()) {
            Map<String, String> newEnv = new HashMap<>(requiredEnv);
            newEnv.remove(key);

            DefaultEventCodec codec = new DefaultEventCodec(newEnv, asStream("input"), new NullOutputStream());

            try{
                codec.readEvent();
                fail("Should have rejected missing env "+ key);
            }catch(FunctionInputHandlingException e){
                assertThat(e).hasMessageContaining("Required environment variable " + key+ " is not set - are you running a function outside of fn run?");
            }
        }

    }

    @Test
    public void shouldWriteOutputDirectlyToOutputStream() throws IOException{

        OutputEvent evt = OutputEvent.fromBytes("hello".getBytes(),OutputEvent.SUCCESS,"text/plain");
        ByteArrayOutputStream bos  = new ByteArrayOutputStream();

        DefaultEventCodec codec = new DefaultEventCodec(new HashMap<>(), new NullInputStream(0),bos);
        codec.writeEvent(evt);
        assertThat(new String(bos.toByteArray())).isEqualTo("hello");

    }
}
