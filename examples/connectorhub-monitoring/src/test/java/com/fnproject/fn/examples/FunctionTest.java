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
    public void testInvokeFunctionWithMetricData() throws Exception {

        ConnectorHubBatch<MetricData> event = createMinimalRequest();
        connectorHubTestFeature.givenEvent(event).enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(200, result.getStatus().getCode());
    }

    private static ConnectorHubBatch<MetricData> createMinimalRequest() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("resourceID", "ocid1.bucket.oc1.xyz");
        dimensions.put("resourceDisplayName", "userName");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("displayName", "PutObject Request Count");
        metadata.put("unit", "count");

        MetricData source = new MetricData(
            "oci_objectstorage",
            "unknown",
            "ocid1.tenancy.oc1..xyz",
            "PutRequests",
            dimensions,
            metadata,
            Collections.singletonList(new Datapoint(new Date(1764860467553L), Double.parseDouble("12.3"), null))
        );
        ConnectorHubBatch<MetricData> event = mock(ConnectorHubBatch.class);

        when(event.getBatch()).thenReturn(Collections.singletonList(source));
        return event;
    }
}