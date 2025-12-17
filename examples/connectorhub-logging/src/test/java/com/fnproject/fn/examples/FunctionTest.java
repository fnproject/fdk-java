package com.fnproject.fn.examples;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.Datapoint;
import com.fnproject.events.input.sch.LoggingData;
import com.fnproject.events.input.sch.MetricData;
import com.fnproject.events.testing.ConnectorHubTestFeature;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class FunctionTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    private final ConnectorHubTestFeature connectorHubTestFeature = ConnectorHubTestFeature.createDefault(fn);

    @Test
    public void testInvokeFunctionWithLoggingData() throws Exception {

        ConnectorHubBatch<LoggingData> event = createMinimalRequest();
        connectorHubTestFeature.givenEvent(event).enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(200, result.getStatus().getCode());
    }

    private static ConnectorHubBatch<LoggingData> createMinimalRequest() {
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

        LoggingData source = new LoggingData(
            "ecb37864-4396-4302-9575-981644949730",
            "log-name",
            "1.0",
            "schedule",
            "com.oraclecloud.functions.application.functioninvoke",
            data,
            oracle,
            new Date(1764860467553L)
        );
        ConnectorHubBatch<LoggingData> event = mock(ConnectorHubBatch.class);

        when(event.getBatch()).thenReturn(Collections.singletonList(source));
        return event;
    }
}