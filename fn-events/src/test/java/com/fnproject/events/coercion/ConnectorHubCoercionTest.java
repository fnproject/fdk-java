package com.fnproject.events.coercion;

import static com.fnproject.events.coercion.APIGatewayCoercion.OM_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.LoggingData;
import com.fnproject.events.input.sch.MetricData;
import com.fnproject.events.input.sch.StreamingData;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.connectorhub.GrandChildMonitorSourceTestFunction;
import com.fnproject.events.testfns.connectorhub.LoggingSourceTestFunction;
import com.fnproject.events.testfns.connectorhub.MonitorSourceTestFunction;
import com.fnproject.events.testfns.connectorhub.QueueSourceObjectTestFunction;
import com.fnproject.events.testfns.connectorhub.QueueSourceStringTestFunction;
import com.fnproject.events.testfns.connectorhub.StreamingSourceObjectTestFunction;
import com.fnproject.events.testfns.connectorhub.StreamingSourceStringTestFunction;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.DefaultMethodWrapper;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import org.junit.Before;
import org.junit.Test;

public class ConnectorHubCoercionTest {
    private ConnectorHubCoercion coercion;
    private InvocationContext requestinvocationContext;

    @Before
    public void setUp() {
        coercion = ConnectorHubCoercion.instance();
        requestinvocationContext = mock(InvocationContext.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        ObjectMapper mapper = new ObjectMapper();

        when(runtimeContext.getAttribute(OM_KEY, ObjectMapper.class)).thenReturn(Optional.of(mapper));
        when(requestinvocationContext.getRuntimeContext()).thenReturn(runtimeContext);
    }

    @Test
    public void testReturnEmptyWhenNotConnectorHubEventClass() {
        MethodWrapper method = new DefaultMethodWrapper(APIGatewayCoercionTest.class, "testMethod");

        Headers headers = Headers.emptyHeaders();

        when(requestinvocationContext.getRequestHeaders()).thenReturn(headers);
        ByteArrayInputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        Optional<ConnectorHubBatch<?>> batch = coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method);

        assertFalse(batch.isPresent());
    }

    @Test
    public void testMonitoringSourceInput() {
        MethodWrapper method = new DefaultMethodWrapper(MonitorSourceTestFunction.class, "handler");

        ConnectorHubBatch<MetricData> event = coerceRequest(method, "[\n" +
            "  {\n" +
            "    \"namespace\": \"oci_objectstorage\",\n" +
            "    \"resourceGroup\": \"nullable\",\n" +
            "    \"compartmentId\": \"ocid1.tenancy.oc1..xyz\",\n" +
            "    \"name\": \"PutRequests\",\n" +
            "    \"dimensions\": {\n" +
            "      \"resourceID\": \"ocid1.bucket.oc1.uk-london-1.xyz\",\n" +
            "      \"resourceDisplayName\": \"foo\"\n" +
            "    },\n" +
            "    \"metadata\": {\n" +
            "      \"displayName\": \"PutObject Request Count\",\n" +
            "      \"unit\": \"count\"\n" +
            "    },\n" +
            "    \"datapoints\": [\n" +
            "      {\n" +
            "        \"timestamp\": 1761318377414,\n" +
            "        \"value\": 1.0,\n" +
            "        \"count\": 1\n" +
            "      }\n" +
            "    ]\n" +
            "  }," +
            "  {\n" +
            "    \"namespace\": \"oci_objectstorage\",\n" +
            "    \"resourceGroup\": null,\n" +
            "    \"compartmentId\": \"ocid1.tenancy.oc1..abc\",\n" +
            "    \"name\": \"PutRequests\",\n" +
            "    \"dimensions\": {\n" +
            "      \"resourceID\": \"ocid1.bucket.oc1.uk-london-1.abc\",\n" +
            "      \"resourceDisplayName\": \"bar\"\n" +
            "    },\n" +
            "    \"metadata\": {\n" +
            "      \"displayName\": \"PutObject Request Count\",\n" +
            "      \"unit\": \"count\"\n" +
            "    },\n" +
            "    \"datapoints\": [\n" +
            "      {\n" +
            "        \"timestamp\": 1761318377414,\n" +
            "        \"value\": 1.0,\n" +
            "        \"count\": 1\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": 1761318377614,\n" +
            "        \"value\": 2.0,\n" +
            "        \"count\": 1\n" +
            "      }\n" +
            "    ]\n" +
            "  }" +
            "]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(2, event.getBatch().size());
        MetricData monitoringSource = event.getBatch().get(0);
        assertEquals("PutRequests", monitoringSource.getName());
        assertEquals("nullable", monitoringSource.getResourceGroup());
        assertEquals("ocid1.tenancy.oc1..xyz", monitoringSource.getCompartmentId());
        assertEquals("oci_objectstorage", monitoringSource.getNamespace());
        assertEquals("ocid1.bucket.oc1.uk-london-1.xyz", monitoringSource.getDimensions().get("resourceID"));
        assertEquals("foo", monitoringSource.getDimensions().get("resourceDisplayName"));
        assertEquals("PutObject Request Count", monitoringSource.getMetadata().get("displayName"));
        assertEquals("count", monitoringSource.getMetadata().get("unit"));
        assertEquals(Integer.valueOf(1), monitoringSource.getDatapoints().get(0).getCount());
        assertEquals(Double.parseDouble("1.0"), monitoringSource.getDatapoints().get(0).getValue(), 0);
        assertEquals(Date.from(Instant.ofEpochMilli(Long.parseLong("1761318377414"))), monitoringSource.getDatapoints().get(0).getTimestamp());
    }

    @Test
    public void testMonitoringSourceInputNoCount() {
        MethodWrapper method = new DefaultMethodWrapper(MonitorSourceTestFunction.class, "handler");

        ConnectorHubBatch<MetricData> event = coerceRequest(method, "[\n" +
            "  {\n" +
            "    \"namespace\":\"oci_computeagent\",\n" +
            "    \"compartmentId\":\"ocid1.tenancy.oc1..exampleuniqueID\",\n" +
            "    \"name\":\"DiskBytesRead\",\n" +
            "    \"dimensions\":{\n" +
            "      \"resourceId\":\"ocid1.instance.region1.phx.exampleuniqueID\"\n" +
            "    },\n" +
            "    \"metadata\":{\n" +
            "      \"unit\":\"bytes\"\n" +
            "    },\n" +
            "  \"datapoints\":[\n" +
            "    {\n" +
            "      \"timestamp\":\"1761318377414\",\n" +
            "      \"value\":10.4\n" +
            "    },\n" +
            "    {\n" +
            "      \"timestamp\":\"1761318377414\",\n" +
            "      \"value\":11.3\n" +
            "    }\n" +
            "  ]\n" +
            " }\n" +
            "]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(1, event.getBatch().size());
        MetricData monitoringSource = event.getBatch().get(0);
        assertEquals("DiskBytesRead", monitoringSource.getName());
        assertEquals("ocid1.tenancy.oc1..exampleuniqueID", monitoringSource.getCompartmentId());
        assertEquals("oci_computeagent", monitoringSource.getNamespace());
        assertNull(monitoringSource.getDimensions().get("resourceID"));
        assertNull(monitoringSource.getDimensions().get("resourceDisplayName"));
        assertNull(monitoringSource.getMetadata().get("displayName"));
        assertEquals("bytes", monitoringSource.getMetadata().get("unit"));
        assertEquals(Double.parseDouble("10.4"), monitoringSource.getDatapoints().get(0).getValue(), 0);
        assertEquals(Date.from(Instant.ofEpochMilli(Long.parseLong("1761318377414"))), monitoringSource.getDatapoints().get(0).getTimestamp());
    }

    @Test
    public void testMonitoringSourceInputEmpty() {
        MethodWrapper method = new DefaultMethodWrapper(MonitorSourceTestFunction.class, "handler");

        ConnectorHubBatch<MetricData> event = coerceRequest(method, "[]");

        assertTrue(event.getBatch().isEmpty());
    }

    @Test
    public void testGrandChildIsCoercedInputEmpty() {
        MethodWrapper method = new DefaultMethodWrapper(GrandChildMonitorSourceTestFunction.class, "handler");

        ConnectorHubBatch<MetricData> event = coerceRequest(method, "[]");

        assertTrue(event.getBatch().isEmpty());
    }

    @Test
    public void testFailureToParseIsUserFriendlyError() {
        MethodWrapper method = new DefaultMethodWrapper(MonitorSourceTestFunction.class, "handler");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> coerceRequest(method, "INVALID JSON"));

        assertEquals(
            "Failed to coerce event to user function parameter type [collection type; class java.util.List, contains [simple type, class com.fnproject.events.input.sch.MetricData]]",
            exception.getMessage());
        assertTrue(exception.getCause().getMessage().startsWith("Unrecognized token 'INVALID':"));
    }

    @Test
    public void testLoggingSourceInput() {
        MethodWrapper method = new DefaultMethodWrapper(LoggingSourceTestFunction.class, "handler");

        ConnectorHubBatch<LoggingData> event = coerceRequest(method, "[\n" +
            "  {\n" +
            "    \"data\": {\n" +
            "      \"applicationId\": \"ocid1.fnapp.oc1.abc\",\n" +
            "      \"containerId\": \"n/a\",\n" +
            "      \"functionId\": \"ocid1.fnfunc.oc1.abc\",\n" +
            "      \"message\": \"Received function invocation request\",\n" +
            "      \"opcRequestId\": \"/abc/def\",\n" +
            "      \"requestId\": \"/def/abc\",\n" +
            "      \"src\": \"stdout\"\n" +
            "    },\n" +
            "    \"id\": \"abc-zyx\",\n" +
            "    \"oracle\": {\n" +
            "      \"compartmentid\": \"ocid1.tenancy.oc1..xyz\",\n" +
            "      \"ingestedtime\": \"2025-10-23T15:45:19.457Z\",\n" +
            "      \"loggroupid\": \"ocid1.loggroup.oc1.abc\",\n" +
            "      \"logid\": \"ocid1.log.oc1.def\",\n" +
            "      \"tenantid\": \"ocid1.tenancy.oc1..xyz\"\n" +
            "    },\n" +
            "    \"source\": \"your-log\",\n" +
            "    \"specversion\": \"1.0\",\n" +
            "    \"subject\": \"schedule\",\n" +
            "    \"time\": \"2025-10-24T15:06:17.000Z\",\n" +
            "    \"type\": \"com.oraclecloud.functions.application.functioninvoke\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"data\": {\n" +
            "      \"applicationId\": \"ocid1.fnapp.oc1.def\",\n" +
            "      \"containerId\": \"n/a\",\n" +
            "      \"functionId\": \"ocid1.fnfunc.oc1.def\",\n" +
            "      \"message\": \"Received function invocation request\",\n" +
            "      \"opcRequestId\": \"/def/xyz\",\n" +
            "      \"requestId\": \"/foo/bar\",\n" +
            "      \"src\": \"stdout\"\n" +
            "    },\n" +
            "    \"id\": \"foo-zyx\",\n" +
            "    \"oracle\": {\n" +
            "      \"compartmentid\": \"ocid1.tenancy.oc1..xyz\",\n" +
            "      \"ingestedtime\": \"2025-11-23T15:45:19.457Z\",\n" +
            "      \"loggroupid\": \"ocid1.loggroup.oc1.def\",\n" +
            "      \"logid\": \"ocid1.log.oc1.xyz\",\n" +
            "      \"tenantid\": \"ocid1.tenancy.oc1..xyz\"\n" +
            "    },\n" +
            "    \"source\": \"your-log\",\n" +
            "    \"specversion\": \"1.0\",\n" +
            "    \"subject\": \"schedule\",\n" +
            "    \"time\": \"2025-11-23T15:45:17.239Z\",\n" +
            "    \"type\": \"com.oraclecloud.functions.application.functioninvoke\"\n" +
            "  }\n" +
            "]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(2, event.getBatch().size());
        LoggingData loggingData = event.getBatch().get(0);
        assertEquals("your-log", loggingData.getSource());
        assertEquals("abc-zyx", loggingData.getId());
        assertEquals("schedule", loggingData.getSubject());
        assertEquals("1.0", loggingData.getSpecversion());
        assertEquals("ocid1.fnapp.oc1.abc", loggingData.getData().get("applicationId"));
        assertEquals("n/a", loggingData.getData().get("containerId"));
        assertEquals("ocid1.fnfunc.oc1.abc", loggingData.getData().get("functionId"));
        assertEquals("Received function invocation request", loggingData.getData().get("message"));
        assertEquals("/abc/def", loggingData.getData().get("opcRequestId"));
        assertEquals("/def/abc", loggingData.getData().get("requestId"));
        assertEquals("stdout", loggingData.getData().get("src"));
        assertEquals("ocid1.tenancy.oc1..xyz", loggingData.getOracle().get("compartmentid"));
        assertEquals("2025-10-23T15:45:19.457Z", loggingData.getOracle().get("ingestedtime"));
        assertEquals("ocid1.loggroup.oc1.abc", loggingData.getOracle().get("loggroupid"));
        assertEquals("ocid1.log.oc1.def", loggingData.getOracle().get("logid"));
        assertEquals("ocid1.tenancy.oc1..xyz", loggingData.getOracle().get("tenantid"));
        assertEquals(Date.from(Instant.ofEpochMilli(Long.parseLong("1761318377000"))), loggingData.getTime());
        assertEquals("com.oraclecloud.functions.application.functioninvoke", loggingData.getType());
    }

    @Test
    public void testStreamingSourceInput() {
        MethodWrapper method = new DefaultMethodWrapper(StreamingSourceStringTestFunction.class, "handler");

        ConnectorHubBatch<StreamingData<String>> event = coerceRequest(method, "[" +
            "{\"stream\":\"stream-name\"," +
            "\"partition\":\"0\"," +
            "\"key\":null," +
            "\"value\":\"U2VudCBhIHBsYWluIG1lc3NhZ2U=\"," +
            "\"offset\":3," +
            "\"timestamp\":1761223385480" +
            "}," +
            "{\"stream\":\"stream-name\"," +
            "\"partition\":\"0\"," +
            "\"key\":null," +
            "\"value\":\"U2VudCBhIHBsYWluIG1lc3NhZ2U=\"," +
            "\"offset\":3," +
            "\"timestamp\":1761223385480" +
            "}" +
            "]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(2, event.getBatch().size());
        StreamingData streamingData = event.getBatch().get(0);
        assertEquals("stream-name", streamingData.getStream());
        assertNull(streamingData.getKey());
        assertEquals("3", streamingData.getOffset());
        assertEquals("0", streamingData.getPartition());
        assertEquals(Date.from(Instant.ofEpochMilli(Long.parseLong("1761223385480"))), streamingData.getTimestamp());
        assertEquals("Sent a plain message", streamingData.getValue());
    }

    @Test
    public void testStreamingSourceInputObject() throws JsonProcessingException {
        MethodWrapper method = new DefaultMethodWrapper(StreamingSourceObjectTestFunction.class, "handler");
        Animal animal = new Animal("cat", 2);

        ConnectorHubBatch<StreamingData<Animal>> event = coerceRequest(method, "[" +
            "{\"stream\":\"stream-name\"," +
            "\"partition\":\"0\"," +
            "\"key\":null," +
            "\"value\":\"" + Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsBytes(animal)) + "\"," +
            "\"offset\":3," +
            "\"timestamp\":1761223385480" +
            "}" +
            "]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(1, event.getBatch().size());
        StreamingData<Animal> streamingData = event.getBatch().get(0);
        assertEquals("stream-name", streamingData.getStream());
        assertNull(streamingData.getKey());
        assertEquals("3", streamingData.getOffset());
        assertEquals("0", streamingData.getPartition());
        assertEquals(animal.getAge(), streamingData.getValue().getAge());
        assertEquals(Date.from(Instant.ofEpochMilli(Long.parseLong("1761223385480"))), streamingData.getTimestamp());
    }

    @Test
    public void testQueueSourceInputInvalidMessage()  {
        MethodWrapper method = new DefaultMethodWrapper(QueueSourceStringTestFunction.class, "handler");
        assertThrows(RuntimeException.class, () -> coerceRequest(method, "[a plain string]"));
    }

    @Test
    public void testQueueSourceInputString()  {
        MethodWrapper method = new DefaultMethodWrapper(QueueSourceStringTestFunction.class, "handler");
        ConnectorHubBatch<String > event = coerceRequest(method, "[\"a plain , comma string 1\",\"a plain , comma string 2\",\"a plain , comma string 3\"]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(3, event.getBatch().size());
        assertEquals("a plain , comma string 1", event.getBatch().get(0));
    }

    @Test
    public void testQueueSourceInputObject() {
        Animal animal = new Animal("cat", 2);
        MethodWrapper method = new DefaultMethodWrapper(QueueSourceObjectTestFunction.class, "handler");
        ConnectorHubBatch<String > event = coerceRequest(method, "[{\"name\": \"cat\",\"age\":2}]");

        assertFalse(event.getBatch().isEmpty());
        assertEquals(1, event.getBatch().size());
        assertEquals(animal, event.getBatch().get(0));
    }

    @Test
    public void testHeaders() {
        MethodWrapper method = new DefaultMethodWrapper(QueueSourceObjectTestFunction.class, "handler");
        when(requestinvocationContext.getRequestHeaders()).thenReturn(Headers.emptyHeaders().addHeader("foo", "bar"));
        ConnectorHubBatch<String > event = coerceRequest(method, "[{\"name\": \"cat\",\"age\":2}]");

        assertEquals("bar", event.getHeaders().get("foo").get());
    }

    @Test
    public void testEmptyHeaders() {
        MethodWrapper method = new DefaultMethodWrapper(QueueSourceObjectTestFunction.class, "handler");
        when(requestinvocationContext.getRequestHeaders()).thenReturn(Headers.emptyHeaders());
        ConnectorHubBatch<String > event = coerceRequest(method, "[{\"name\": \"cat\",\"age\":2}]");

        assertEquals(0, event.getHeaders().asMap().size());
    }

    private ConnectorHubBatch coerceRequest(MethodWrapper method, String body) {
        ByteArrayInputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        InputEvent inputEvent = new ReadOnceInputEvent(is, Headers.emptyHeaders(), "call", Instant.now());
        return coercion.tryCoerceParam(requestinvocationContext, 0, inputEvent, method).orElseThrow(RuntimeException::new);
    }
}