# Example Fn Java FDK : Service Connector Hub - Queue

This example provides a Function to use as a service connector hub target.
The function accepts a typed event containing a batch of source messages.

## Source
[LoggingData.java](../../fn-events/src/main/java/com/fnproject/events/input/sch/LoggingData.java)

## Dependencies
* [fn-events] for ConnectorHubFunction classes.
* [fn-events-testing] for ConnectorHubFunction testing library.

## Demonstrated FDK features

This example showcases how to use the fn-event ConnectorHubFunction to 
use a Function as the target for Queue source.

## Step by step

Set up the connector hub with Logging source and Function target:
* [Setup default policies](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#Authenti__default-policies)
* [create connector hub](https://docs.oracle.com/en-us/iaas/Content/connector-hub/create-service-connector-queue-source.htm)

The Function entrypoint extends the `ConnectorHubFunction` abstract class.
Note: the [func.yaml](func.yaml) entrypoint remains the class which extends `ConnectorHubFunction` 
e.g. `cmd: com.fnproject.fn.examples.Function::handler`

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)

```java
package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;

public class Function extends ConnectorHubFunction<Employee> {

    public QueueService queueService;

    public Function() {
        this.queueService = new QueueService();
    }

    @Override
    public void handler(ConnectorHubBatch<Employee> batch) {
        for (Employee employee : batch.getBatch()) {
            queueService.readContent(employee);
        }
    }
}
```
The [ConnectorHubBatch.java](../../fn-events/src/main/java/com/fnproject/events/input/ConnectorHubBatch.java) 
`batch` contains a list of messages from Queue as 
specified in [Batch Settings](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#batch-settings).

The class [Employee.java](src/main/java/com/fnproject/fn/examples/Employee.java) is 
a representation of messages received from the Queue.

Note the messages sent in to the Queue must be a valid String because Connector Hub forwards the message directly to 
the Function target.
- wrong: a plain string
- correct: "a plain string"

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
`public class Function extends ConnectorHubFunction<Employee>`.

To return an error response, throw RuntimeException.class.
Doing so will cause the Function to return a 502 [Retry policy](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#deactivate)

## Test walkthrough

Unit testing `ConnectorHubFunction` is supported with the `ConnectorHubTestFeature` and `FnTestingRule`.

First of all, the class initializes the `FnTestingRule` harness, as explained
in [Testing Functions](../../docs/TestingFunctions.md).

[FunctionTest.java](src/test/java/com/fnproject/fn/examples/FunctionTest.java)
```java
package com.fnproject.fn.examples;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import com.fnproject.events.input.ConnectorHubBatch;
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

        ConnectorHubBatch<Employee> event = createMinimalRequest();
        connectorHubTestFeature.givenEvent(event).enqueue();

        fn.thenRun(Function.class, "handler");

        FnResult result = fn.getOnlyResult();
        assertEquals(200, result.getStatus().getCode());
    }

    private static ConnectorHubBatch<Employee> createMinimalRequest() {
        Employee employee = new Employee();
        employee.setName("foo");

        ConnectorHubBatch<Employee> event = mock(ConnectorHubBatch.class);

        when(event.getBatch()).thenReturn(Collections.singletonList(employee));
        return event;
    }
}
```

Use `connectorHubTestFeature.givenEvent(event).enqueue();` to queue the request event 
and invoke the Function with `fn.thenRun(Function.class, "handler");`.
