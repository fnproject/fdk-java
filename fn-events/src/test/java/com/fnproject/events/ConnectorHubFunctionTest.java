package com.fnproject.events;

import static org.junit.Assert.assertEquals;
import java.util.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.connectorhub.LoggingSourceTestFunction;
import com.fnproject.events.testfns.connectorhub.MonitorSourceTestFunction;
import com.fnproject.events.testfns.connectorhub.QueueSourceObjectTestFunction;
import com.fnproject.events.testfns.connectorhub.QueueSourceStringTestFunction;
import com.fnproject.events.testfns.connectorhub.StreamingSourceObjectTestFunction;
import com.fnproject.events.testfns.connectorhub.StreamingSourceStringTestFunction;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class ConnectorHubFunctionTest {

    @Rule
    public final FnTestingRule fnRule = FnTestingRule.createDefault();

    @Test
    public void testMonitorSourceTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[\n" +
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
                "]")
            .enqueue();

        fnRule.thenRun(MonitorSourceTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testEmptyMonitorSourceTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[]")
            .enqueue();

        fnRule.thenRun(MonitorSourceTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testLoggingSourceTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[\n" +
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
                "    \"time\": \"2025-10-23T15:45:17.239Z\",\n" +
                "    \"type\": \"com.oraclecloud.functions.application.functioninvoke\"\n" +
                "  }\n" +
                "]")
            .enqueue();

        fnRule.thenRun(LoggingSourceTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testEmptyLoggingSourceTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[]")
            .enqueue();

        fnRule.thenRun(LoggingSourceTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testStreamingSourceStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[" +
                "{\"stream\":\"stream-name\"," +
                "\"partition\":\"0\"," +
                "\"key\":null," +
                "\"value\":\"U2VudCBhIHBsYWluIG1lc3NhZ2U=\"," +
                "\"offset\":3," +
                "\"timestamp\":1761223385480" +
                "}" +
                "]")
            .enqueue();

        fnRule.thenRun(StreamingSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testEmptyStreamingSourceStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[]")
            .enqueue();

        fnRule.thenRun(StreamingSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testStreamingSourceObjectTestFunction() throws JsonProcessingException {
        Animal animal = new Animal("foo", 4);
        String encodedAnimal = Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsBytes(animal));
        fnRule
            .givenEvent()
            .withBody("[" +
                "{\"stream\":\"stream-name\"," +
                "\"partition\":\"0\"," +
                "\"key\":null," +
                "\"value\":\"" + encodedAnimal + "\"," +
                "\"offset\":3," +
                "\"timestamp\":1761223385480" +
                "}" +
                "]")
            .enqueue();

        fnRule.thenRun(StreamingSourceObjectTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testQueueSourceStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[\"a plain string, end\", \"another string\"]")
            .enqueue();

        fnRule.thenRun(QueueSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testInvalidQueueSourceStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[a plain string, end, another string]")
            .enqueue();

        fnRule.thenRun(QueueSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(2, exitCode);
    }

    @Test
    public void testQueueSourceEmptyTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[]")
            .enqueue();

        fnRule.thenRun(QueueSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testQueueSourceObjectTestFunction() {
        fnRule
            .givenEvent()
            .withBody("[{\"name\":\"foo\",\"age\":3}]")
            .enqueue();

        fnRule.thenRun(QueueSourceObjectTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testSourceWithoutBodyThrows() {
        fnRule
            .givenEvent()
            .enqueue();

        fnRule.thenRun(QueueSourceStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(2, exitCode);
    }
}