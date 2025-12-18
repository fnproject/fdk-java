package com.fnproject.events.testing;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.Datapoint;
import com.fnproject.events.input.sch.LoggingData;
import com.fnproject.events.input.sch.MetricData;
import com.fnproject.events.input.sch.StreamingData;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.testing.FnTestingRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

public class ConnectorHubTestFeatureTest {

    @Rule
    public final FnTestingRule fn = FnTestingRule.createDefault();

    ConnectorHubTestFeature feature = ConnectorHubTestFeature.createDefault(fn);

    @Test
    public void testMetricDataBody() throws Exception {
        ConnectorHubBatch<MetricData> req = new ConnectorHubBatch<>(Collections.singletonList(
            new MetricData(
                "ns",
                null,
                "compartment",
                "name",
                new HashMap<>(),
                new HashMap<>(),
                Collections.singletonList(new Datapoint(
                    new Date(1764860467553L),
                    Double.parseDouble("1.2"),
                    null)
                )
            )
        ), Headers.emptyHeaders());

        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals(
            "[{\"namespace\":\"ns\",\"resourceGroup\":null,\"compartmentId\":\"compartment\",\"name\":\"name\",\"dimensions\":{},\"metadata\":{},\"datapoints\":[{\"timestamp\":1764860467553,\"value\":1.2,\"count\":null}]}]",
            body);
    }

    @Test
    public void testMetricEmptyList() throws Exception {
        ConnectorHubBatch<MetricData> req = new ConnectorHubBatch<>(Collections.emptyList(), Headers.emptyHeaders());
        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals("[]", body);
    }

    @Test
    public void testLoggingDataBody() throws Exception {
        Map<String, String> data = new HashMap();
        data.put("applicationId", "ocid1.fnapp.oc1.xyz");
        data.put("containerId", "n/a");
        data.put("functionId", "ocid1.fnfunc.oc1.xyz");
        data.put("message", "Received function invocation request");
        data.put("opcRequestId", "/abc/def");
        data.put("requestId", "/def/abc");
        data.put("src", "STDOUT");

        Map<String, String> oracle = new HashMap();
        oracle.put("compartmentid", "ocid1.tenancy.oc1.xyz");
        oracle.put("ingestedtime", "2025-10-23T15:45:19.457Z");
        oracle.put("loggroupid", "ocid1.loggroup.oc1.abc");
        oracle.put("logid", "ocid1.log.oc1.abc");
        oracle.put("tenantid", "ocid1.tenancy.oc1.xyz");

        ConnectorHubBatch<LoggingData> req = new ConnectorHubBatch<>(Collections.singletonList(
            new LoggingData(
                "ecb37864-4396-4302-9575-981644949730",
                "log-name",
                "1.0",
                "schedule",
                "com.oraclecloud.functions.application.functioninvoke",
                data,
                oracle,
                new Date(1764860467553L)
            )
        ), Headers.emptyHeaders());

        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals(
            "[{\"id\":\"ecb37864-4396-4302-9575-981644949730\",\"source\":\"log-name\",\"specversion\":\"1.0\",\"subject\":\"schedule\",\"type\":\"com.oraclecloud.functions.application.functioninvoke\",\"data\":{\"functionId\":\"ocid1.fnfunc.oc1.xyz\",\"opcRequestId\":\"/abc/def\",\"src\":\"STDOUT\",\"requestId\":\"/def/abc\",\"applicationId\":\"ocid1.fnapp.oc1.xyz\",\"containerId\":\"n/a\",\"message\":\"Received function invocation request\"},\"oracle\":{\"compartmentid\":\"ocid1.tenancy.oc1.xyz\",\"ingestedtime\":\"2025-10-23T15:45:19.457Z\",\"loggroupid\":\"ocid1.loggroup.oc1.abc\",\"tenantid\":\"ocid1.tenancy.oc1.xyz\",\"logid\":\"ocid1.log.oc1.abc\"},\"time\":1764860467553}]",
            body);
    }

    @Test
    public void testLoggingDataEmptyList() throws Exception {
        ConnectorHubBatch<LoggingData> req = new ConnectorHubBatch<>(Collections.emptyList(), Headers.emptyHeaders());
        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals("[]", body);
    }

    @Test
    public void testStreamingBody() throws Exception {
        Animal animal = new Animal("foo", 4);

        StreamingData<Animal> source = new StreamingData<>(
            "stream-name",
            "0",
            null,
            animal,
            "3",
            new Date(1764860467553L)
        );

        ConnectorHubBatch<StreamingData<Animal>> req = new ConnectorHubBatch<>(Collections.singletonList(
            source
        ), Headers.emptyHeaders());

        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals(
            "[{\"stream\":\"stream-name\",\"partition\":\"0\",\"key\":null,\"value\":{\"name\":\"foo\",\"age\":4},\"offset\":\"3\",\"timestamp\":1764860467553}]",
            body);
    }

    @Test
    public void testStreamingStringBody() throws Exception {
        StreamingData<String> source = new StreamingData<>(
            "stream-name",
            "0",
            null,
            "a plain string",
            "3",
            new Date(1764860467553L)
        );

        ConnectorHubBatch<StreamingData<String>> req = new ConnectorHubBatch<>(Collections.singletonList(
            source
        ), Headers.emptyHeaders());

        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals(
            "[{\"stream\":\"stream-name\",\"partition\":\"0\",\"key\":null,\"value\":\"a plain string\",\"offset\":\"3\",\"timestamp\":1764860467553}]",
            body);
    }

    @Test
    public void testStreamingDataEmptyList() throws Exception {
        ConnectorHubBatch<StreamingData<String>> req = new ConnectorHubBatch<>(Collections.emptyList(), Headers.emptyHeaders());
        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        String body = inputEvent.consumeBody(is -> {
            try {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Error reading input as string");
            }
        });
        assertEquals("[]", body);
    }

    @Test
    public void testHeadersEmptyList() throws Exception {
        ConnectorHubBatch<StreamingData<String>> req = new ConnectorHubBatch<>(Collections.emptyList(), Headers.emptyHeaders());
        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals(Headers.emptyHeaders(), inputEvent.getHeaders());
    }

    @Test
    public void testHeadersList() throws Exception {
        Headers headers = Headers.emptyHeaders().addHeader("foo", "bar");
        ConnectorHubBatch<StreamingData<String>> req = new ConnectorHubBatch<>(Collections.emptyList(), headers);
        ConnectorHubTestFeature.ConnectorHubRequestEventBuilder builder = (ConnectorHubTestFeature.ConnectorHubRequestEventBuilder) feature.givenEvent(req);
        InputEvent inputEvent = builder.build();

        assertEquals("bar", inputEvent.getHeaders().get("foo").get());
    }
}