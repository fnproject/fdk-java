package com.fnproject.fn.runtime.codec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.QueryParametersParser;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An {@link EventCodec} which reads JSON-format events from the {@link java.io.InputStream}.
 * <p>
 * This consumes and closes the {@link java.io.InputStream}
 */
public class JsonEventCodec implements EventCodec {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsonEvent {
        @JsonProperty("protocol")
        Protocol protocol;
        @JsonProperty("body")
        String body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Protocol {
        @JsonProperty("request_url")
        String requestUrl;
        @JsonProperty("method")
        String method;
        @JsonProperty("headers")
        Map<String, List<String>> headers;
    }


    private final Map<String, String> env;
    private final InputStream in;
    private final OutputStream out;

    public JsonEventCodec(Map<String, String> env, InputStream in, OutputStream out) {
        this.env = env;
        this.in = in;
        this.out = out;
    }

    private String getRequiredEnv(String name) {
        String val = env.get(name);
        if (val == null) {
            throw new FunctionInputHandlingException("Required environment variable " + name + " is not set - are you running a function outside of fn run?");
        }
        return val;
    }

    private Map<String, String> collapseHeaders(Map<String, List<String>> headers){
        Map<String, String> collapsedHeaders = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()){
            collapsedHeaders.put(entry.getKey(), String.join(",", entry.getValue()));
        }
        return collapsedHeaders;
    }

    @Override
    public Optional<InputEvent> readEvent() {
        try {
            JsonEvent jsonEvent = new ObjectMapper().readValue(in, JsonEvent.class);
            InputEvent inputEvent = new ReadOnceInputEvent(
                    getRequiredEnv("FN_APP_NAME"),
                    getRequiredEnv("FN_PATH"),
                    jsonEvent.protocol.requestUrl,
                    jsonEvent.protocol.method,
                    IOUtils.toInputStream(jsonEvent.body, StandardCharsets.UTF_8),
                    Headers.fromMap(collapseHeaders(jsonEvent.protocol.headers)),
                    QueryParametersParser.getParams(jsonEvent.protocol.requestUrl));

            return Optional.of(inputEvent);
        } catch (Exception e) {
            throw new FunctionInputHandlingException("Failed to read JSON content from input", e);
        }
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public void writeEvent(OutputEvent evt) {

    }
}
