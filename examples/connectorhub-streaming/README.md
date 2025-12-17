# Example Fn Java FDK : Service Connector Hub - Streaming

This example provides a Function to use as a service connector hub target.
The function accepts a typed event containing a batch of messages.

## Source
[Streaming](https://docs.oracle.com/en-us/iaas/api/#/en/streaming/20180418/Message)
The value in each streaming event is delivered as base64 encoded. The library automatically decodes it.

## Dependencies
* [fn-events] for ConnectorHubFunction classes.
* [fn-events-testing] for ConnectorHubFunction testing library.

## Demonstrated FDK features

This example showcases how to use the fn-event ConnectorHubFunction to 
use a Function as the target for Streaming source.

## Step by step

Set up the connector hub with Streaming source and Function target:
* [Setup default policies](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#Authenti__default-policies)
* [create connector hub](https://docs.oracle.com/en-us/iaas/Content/connector-hub/create-service-connector-streaming-source.htm)

The Function entrypoint extends the `ConnectorHubFunction` abstract class.
Note: the [func.yaml](func.yaml) entrypoint remains the class which extends `ConnectorHubFunction` 
e.g. `cmd: com.fnproject.fn.examples.Function::handler`

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)

```java
package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.StreamingData;

public class Function extends ConnectorHubFunction<StreamingData<Employee>> {

    public StreamService streamService;

    public Function() {
        this.streamService = new StreamService();
    }

    @Override
    public void handler(ConnectorHubBatch<StreamingData<Employee>> batch) {
        for (StreamingData<Employee> stream : batch.getBatch()) {
            streamService.readStream(stream);
        }
    }
}
```
The [ConnectorHubBatch.java](../../fn-events/src/main/java/com/fnproject/events/input/ConnectorHubBatch.java) 
`batch` contains a list of events from Streaming as 
specified in [Batch Settings](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#batch-settings).

The class [StreamingData.java](../../fn-events/src/main/java/com/fnproject/events/input/sch/StreamingData.java)
represents the batch of Streaming Events. 

The [Employee.java](src/main/java/com/fnproject/fn/examples/Employee.java) represents the base64 encoded JSON
from value in each message. Note: Provide a String type if the message value is not JSON format. 

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
`public class Function extends ConnectorHubFunction<StreamingData<Employee>>`.

To return an error response, throw RuntimeException.class.
Doing so will cause the Function to return a 502 [Retry policy](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#deactivate)

## Test walkthrough

Unit testing `ConnectorHubFunction` is supported with the `ConnectorHubTestFeature` and `FnTestingRule`.

First of all, the class initializes the `FnTestingRule` harness, as explained
in [Testing Functions](../../docs/TestingFunctions.md).

[FunctionTest.java](src/test/java/com/fnproject/fn/examples/FunctionTest.java)
```java
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

    private static ConnectorHubBatch<StreamingData<Employee>> createMinimalRequest() throws JsonProcessingException {
        Employee employee = new Employee();
        employee.setName("foo");

        StreamingData<Employee> source = new StreamingData<Employee>(
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
```

Use `connectorHubTestFeature.givenEvent(event).enqueue();` to queue the request event 
and invoke the Function with `fn.thenRun(Function.class, "handler");`.
