package com.fnproject.fn.examples;

import static org.junit.Assert.assertEquals;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.events.testing.NotificationTestFeature;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class FunctionTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    private final NotificationTestFeature connectorHubTestFeature = NotificationTestFeature.createDefault(fn);

    @Test
    public void testInvokeFunctionWithLoggingData() throws Exception {
        NotificationMessage<Employee> event = createMinimalRequest();
        connectorHubTestFeature.givenEvent(event).enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(200, result.getStatus().getCode());
    }

    private static NotificationMessage createMinimalRequest() {
        Employee employee = new Employee();
        employee.setName("foo");

        return new NotificationMessage<>(employee, Headers.emptyHeaders());
    }
}