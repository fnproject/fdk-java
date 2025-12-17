# Example Fn Java FDK : Service Connector Hub - Logging

This example provides a Function to use as a service connector hub target.
The function accepts a typed event containing a batch of source events.

## Source
[LoggingData.java](../../fn-events/src/main/java/com/fnproject/events/input/sch/LoggingData.java)

## Dependencies
* [fn-events] for ConnectorHubFunction classes.
* [fn-events-testing] for ConnectorHubFunction testing library.

## Demonstrated FDK features

This example showcases how to use the fn-event ConnectorHubFunction to 
use a Function as the target for Logging source.

## Step by step

Set up the connector hub with Logging source and Function target:
* [Setup default policies](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#Authenti__default-policies)
* [create connector hub](https://docs.oracle.com/en-us/iaas/Content/connector-hub/create-service-connector-logging-source.htm)

The Function entrypoint extends the `ConnectorHubFunction` abstract class.
Note: the [func.yaml](func.yaml) entrypoint remains the class which extends `ConnectorHubFunction` 
e.g. `cmd: com.fnproject.fn.examples.Function::handler`

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
```java
package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.LoggingData;

public class Function extends ConnectorHubFunction<LoggingData> {

    public LogService logService;

    public Function() {
        this.logService = new LogService();
    }

    @Override
    public void handler(ConnectorHubBatch<LoggingData> batch) {
        for (LoggingData log : batch.getBatch()) {
            logService.readLog(log);
        }
    }
}
```
The [ConnectorHubBatch.java](../../fn-events/src/main/java/com/fnproject/events/input/ConnectorHubBatch.java) 
`batch` contains a list of events from Logging as 
specified in [Batch Settings](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#batch-settings).

The class [LoggingData.java](../../fn-events/src/main/java/com/fnproject/events/input/sch/LoggingData.java) is 
each logging event.

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
`public class Function extends ConnectorHubFunction<LoggingData>`.

To return an error response, throw RuntimeException.class.
Doing so will cause the Function to return a 502 [Retry policy](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#deactivate)

## Test walkthrough

Unit testing `ConnectorHubFunction` is supported with the `ConnectorHubTestFeature` and `FnTestingRule`.

First of all, the class initializes the `FnTestingRule` harness, as explained
in [Testing Functions](../../docs/TestingFunctions.md).

[FunctionTest.java](src/test/java/com/fnproject/fn/examples/FunctionTest.java)
```java
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
```

Use `connectorHubTestFeature.givenEvent(event).enqueue();` to queue the request event 
and invoke the Function with `fn.thenRun(Function.class, "handler");`.
