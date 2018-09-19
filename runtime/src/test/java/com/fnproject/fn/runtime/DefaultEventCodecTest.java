package com.fnproject.fn.runtime;

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

public class DefaultEventCodecTest {

    private final Map<String, String> emptyConfig = new HashMap<>();
    private InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    public void shouldExtractBasicEvent() {
        Map<String, String> env = new HashMap<>();
        env.put("FN_FORMAT", "default");
        env.put("FN_HEADER_CONTENT_TYPE", "text/plain");
        env.put("FN_HEADER_ACCEPT", "text/html, text/plain;q=0.9");
        env.put("FN_HEADER_ACCEPT_ENCODING", "gzip");
        env.put("FN_HEADER_USER_AGENT", "userAgent");

        Map<String, String> config = new HashMap<>();
        config.put("configparam", "configval");
        config.put("CONFIGPARAM", "CONFIGVAL");

        DefaultEventCodec codec = new DefaultEventCodec(env, asStream("input"), new NullOutputStream());
        InputEvent evt = codec.readEvent();


        assertThat(evt.getHeaders().asMap().size()).isEqualTo(4);
        assertThat(evt.getHeaders().asMap()).contains(
                headerEntry("CONTENT_TYPE", "text/plain"),
                headerEntry("ACCEPT_ENCODING", "gzip"),
                headerEntry("ACCEPT", "text/html, text/plain;q=0.9"),
                headerEntry("USER_AGENT", "userAgent"));

        evt.consumeBody((body) -> assertThat(body).hasSameContentAs(asStream("input")));
    }



    @Test
    public void shouldWriteOutputDirectlyToOutputStream() throws IOException{

        OutputEvent evt = OutputEvent.fromBytes("hello".getBytes(),OutputEvent.Status.Success,"text/plain");
        ByteArrayOutputStream bos  = new ByteArrayOutputStream();

        DefaultEventCodec codec = new DefaultEventCodec(new HashMap<>(), new NullInputStream(0),bos);
        codec.writeEvent(evt);
        assertThat(new String(bos.toByteArray())).isEqualTo("hello");

    }
}
