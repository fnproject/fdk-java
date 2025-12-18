package com.fnproject.events.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URLEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.testing.FnEventBuilder;
import com.fnproject.fn.testing.FnEventBuilderJUnit4;
import com.fnproject.fn.testing.FnHttpEventBuilder;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingClassLoader;
import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.fn.testing.FnTestingRuleFeature;

public class APIGatewayTestFeature implements FnTestingRuleFeature {
    private final FnTestingRule rule;
    private final ObjectMapper mapper;

    private APIGatewayTestFeature(FnTestingRule rule) {
        this(rule, new ObjectMapper());
    }

    private APIGatewayTestFeature(FnTestingRule rule, ObjectMapper mapper) {
        this.rule = rule;
        this.mapper = mapper;
    }

    public static APIGatewayTestFeature createDefault(FnTestingRule rule) {
        APIGatewayTestFeature feature = new APIGatewayTestFeature(rule);
        rule.addFeature(feature);
        return feature;
    }

    @Override
    public void prepareTest(ClassLoader functionClassLoader, PrintStream stderr, String cls, String method) {

    }

    @Override
    public void prepareFunctionClassLoader(FnTestingClassLoader cl) {

    }

    @Override
    public void afterTestComplete() {

    }

    public FnEventBuilderJUnit4 givenEvent(APIGatewayRequestEvent event) throws JsonProcessingException {
        return new APIGatewayFnEventBuilder(event);
    }

    /*
        Unwrap output event to APIGatewayResponseEvent type
     */
    public <T> APIGatewayResponseEvent<T> getResult(Class<T> tClass) throws IOException {
        FnResult result = rule.getOnlyResult();
        APIGatewayResponseEvent.Builder<T> responseBuilder =
            new APIGatewayResponseEvent.Builder<T>();

        if (result.getBodyAsBytes().length != 0) {
            T body = mapper.readValue(result.getBodyAsBytes(), tClass);
            responseBuilder
                .body(body);
        }

        Map<String, List<String>> myHeaders = new HashMap<>();
        result.getHeaders().asMap().forEach((key, headerValues) -> {
            if (key.startsWith("Fn-Http-H-")) {
                String httpKey = key.substring("Fn-Http-H-".length());
                if (!httpKey.isEmpty()) {
                    myHeaders.put(httpKey, headerValues);
                }
            }
        });
        responseBuilder.headers(Headers.emptyHeaders().setHeaders(myHeaders));

        if (result.getHeaders().get("Fn-Http-Status").isPresent()) {
            int statusCode = Integer.parseInt(result.getHeaders().get("Fn-Http-Status").get());
            responseBuilder.statusCode(statusCode);
        }
        return responseBuilder.build();
    }

    class APIGatewayFnEventBuilder implements FnEventBuilderJUnit4 {

        FnHttpEventBuilder builder = new FnHttpEventBuilder();

        APIGatewayFnEventBuilder(APIGatewayRequestEvent requestEvent) throws JsonProcessingException {
            withBody(mapper.writeValueAsBytes(requestEvent.getBody()));
            if (requestEvent.getMethod() != null) {
                withHeader("Fn-Http-Method", requestEvent.getMethod());
            }

            if (requestEvent.getHeaders() != null) {
                requestEvent.getHeaders().asMap().forEach((key, headerValues) -> {
                    key = "Fn-Http-H-" + key;
                    for (String headerValue : headerValues) {
                        withHeader(key, headerValue);
                    }
                });
            }

            /*
                This wraps the test query parameters object into a format the FunctionInvocationContext.class can consume.
             */
            String baseUrl = requestEvent.getRequestUrl() != null ? requestEvent.getRequestUrl() : "";
            Map<String, List<String>> params = requestEvent.getQueryParameters() != null
                ? requestEvent.getQueryParameters().getAll()
                : Collections.emptyMap();

            String query = params.entrySet().stream()
                .flatMap(e -> {
                    String k = urlEncode(e.getKey());
                    List<String> vs = e.getValue();
                    if (vs == null || vs.isEmpty()) return Stream.of(k + "=");
                    return vs.stream().map(v -> k + "=" + urlEncode(v));
                })
                .collect(Collectors.joining("&"));

            withHeader("Fn-Http-Request-Url", query.isEmpty() ? baseUrl : baseUrl + "?" + query);
        }

        @Override
        public FnEventBuilder withHeader(String key, String value) {
            builder.withHeader(key, value);
            return this;
        }

        @Override
        public FnEventBuilder withBody(InputStream body) throws IOException {
            builder.withBody(body);
            return this;
        }

        @Override
        public FnEventBuilder withBody(byte[] body) {
            builder.withBody(body);
            return this;
        }

        @Override
        public FnEventBuilder withBody(String body) {
            builder.withBody(body);
            return this;
        }

        @Override
        public FnTestingRule enqueue() {
            rule.addInput(builder.buildEvent());

            return rule;
        }

        @Override
        public FnTestingRule enqueue(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("Invalid count");
            }
            for (int i = 0; i < n; i++) {
                enqueue();
            }
            return rule;
        }

        InputEvent build() {
            return builder.buildEvent();
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
