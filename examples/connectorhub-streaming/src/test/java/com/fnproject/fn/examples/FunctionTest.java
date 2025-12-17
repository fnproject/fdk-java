package com.fnproject.fn.examples;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Date;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.StreamingData;
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
    public void testInvokeFunctionWithStreamingData() throws Exception {

        ConnectorHubBatch<StreamingData<Employee>> event = createMinimalRequest();
        connectorHubTestFeature.givenEvent(event).enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(200, result.getStatus().getCode());
    }

    private static ConnectorHubBatch<StreamingData<Employee>> createMinimalRequest() {
        Employee employee = new Employee();
        employee.setName("foo");

        StreamingData<Employee> source = new StreamingData<>(
            "stream-name",
            "0",
            null,
            employee,
            "3",
            new Date(1764860467553L)
        );
        ConnectorHubBatch<StreamingData<Employee>> event = mock(ConnectorHubBatch.class);

        when(event.getBatch()).thenReturn(Collections.singletonList(source));
        return event;
    }
}