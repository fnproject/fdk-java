package com.fnproject.events.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.testing.FnEventBuilder;
import com.fnproject.fn.testing.FnEventBuilderJUnit4;
import com.fnproject.fn.testing.FnHttpEventBuilder;
import com.fnproject.fn.testing.FnTestingClassLoader;
import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.fn.testing.FnTestingRuleFeature;

public class NotificationTestFeature implements FnTestingRuleFeature {
    private final FnTestingRule rule;
    private final ObjectMapper mapper;

    private NotificationTestFeature(FnTestingRule rule) {
        this(rule, new ObjectMapper());
    }

    private NotificationTestFeature(FnTestingRule rule, ObjectMapper mapper) {
        this.rule = rule;
        this.mapper = mapper;
    }

    public static NotificationTestFeature createDefault(FnTestingRule rule) {
        NotificationTestFeature feature = new NotificationTestFeature(rule);
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

    public FnEventBuilderJUnit4 givenEvent(NotificationMessage<?> event) throws JsonProcessingException {
        return new NotificationRequestEventBuilder(event);
    }

    class NotificationRequestEventBuilder implements FnEventBuilderJUnit4 {

        FnHttpEventBuilder builder = new FnHttpEventBuilder();

        NotificationRequestEventBuilder(NotificationMessage<?> requestEvent) throws JsonProcessingException {
            withBody(mapper.writeValueAsBytes(requestEvent.getContent()));
            if (requestEvent.getHeaders() != null) {
                requestEvent.getHeaders().asMap().forEach((key, headerValues) -> {
                    for (String headerValue : headerValues) {
                        withHeader(key, headerValue);
                    }
                });
            }
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
}
