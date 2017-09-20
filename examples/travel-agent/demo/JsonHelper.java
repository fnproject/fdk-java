package com.example.fn;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(JsonHelper.class);

    static <RespT> FlowFuture<RespT> wrapJsonFunction(String name, Object input, Class<RespT> result) {
        byte[] bytes = toJson(input);

        JsonHelper.log.info("Calling {} with {}:{}", name, input, new String(bytes));
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("NO_CHAIN", "true");

        return Flows.currentFlow().invokeFunction(name, HttpMethod.POST, Headers.fromMap(headerMap), bytes)
                .thenApply((httpResp) -> fromJson(httpResp.getBodyAsBytes(), result))
                .whenComplete((v, e) -> {
                    if (e != null) {
                        JsonHelper.log.error("Got error from {} ", name, e);
                    } else {
                        JsonHelper.log.info("Got response from {}: {}", name, v);
                    }
                });

    }

    static FlowFuture<Void> wrapJsonFunction(String name, Object input) {
        byte[] bytes = toJson(input);
        JsonHelper.log.info("Calling {} with {} : {}", name, input, new String(bytes));

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("NO_CHAIN", "true");

        return Flows.currentFlow().invokeFunction(name, HttpMethod.POST, Headers.fromMap(headerMap), bytes)
                .handle((v, e) -> {
                    if (e != null) {
                        JsonHelper.log.error("Got error from {} ", name, e);
                    } else {
                        JsonHelper.log.info("Got response from {}: {}", name, v);
                    }
                    return null;
                });

    }

    private static <T> T fromJson(byte[] data, Class<T> type) {
        try {
            return JsonHelper.objectMapper.readValue(data, type);
        } catch (IOException e) {
            JsonHelper.log.error("Failed to extract value to {} ", type, e);
            throw new RuntimeException(e);
        }
    }

    private static <T> byte[] toJson(T val) {
        try {
            return JsonHelper.objectMapper.writeValueAsString(val).getBytes();
        } catch (IOException e) {
            JsonHelper.log.error("Failed to wite {} to json ", val, e);
            throw new RuntimeException(e);
        }
    }
}
