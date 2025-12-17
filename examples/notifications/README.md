# Example Fn Java FDK : Notifications

This example provides a Function to consume a Notification.
The function accepts a typed message event.

## Source

* Notifications appends metadata to the headers [Standard header metadata](https://docs.oracle.com/en-us/iaas/Content/Notification/Concepts/notificationoverview.htm#hownw)
* The Notification message body is a user specified Schema or String.
Note: This library currently supports JSON and String types.
* Limits for Notifications [Notification limits](https://docs.oracle.com/en-us/iaas/Content/Notification/Concepts/notificationoverview.htm#limits)

## Dependencies
* [fn-events](../../fn-events) for NotificationFunction classes.
* [fn-events-testing](../../fn-events-testing) for NotificationFunction testing library.

## Demonstrated FDK features

This example showcases how to use the fn-event NotificationFunction to 
use a Function to consume messages from a Notification topic.

## Step by step

Set up the Notification Topic with Function Subscription:
* [Setup default policies](https://docs.oracle.com/en-us/iaas/Content/Security/Reference/notifications_security.htm#iam-policies__subs)
* [create subscription function](https://docs.oracle.com/en-us/iaas/Content/Notification/Tasks/create-subscription-function.htm)

The Function entrypoint extends the `NotificationFunction` abstract class.
Note: the [func.yaml](func.yaml) entrypoint remains the class which extends `NotificationFunction` 
e.g. `cmd: com.fnproject.fn.examples.Function::handler`

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)

```java
package com.fnproject.fn.examples;

import com.fnproject.events.NotificationFunction;
import com.fnproject.events.input.NotificationMessage;

public class Function extends NotificationFunction<Employee> {

    public NotificationService notificationService;

    public Function() {
        this.notificationService = new NotificationService();
    }

    @Override
    public void handler(NotificationMessage<Employee> content) {
        notificationService.readNotification(content);
    }
}
```
The [NotificationMessage.java](../../fn-events/src/main/java/com/fnproject/events/input/NotificationMessage.java) 
`content` contains the message body.

The class [Employee.java](src/main/java/com/fnproject/fn/examples/Employee.java) is 
the schema for the message content.

[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
`public class Function extends NotificationFunction<Employee>`.

To return an error response, throw RuntimeException.class.
Doing so will cause the Function to return a 502 [Retry policy](https://docs.oracle.com/en-us/iaas/Content/connector-hub/overview.htm#deactivate)

## Test walkthrough

Unit testing `NotificationFunction` is supported with the `NotificationTestFeature` and `FnTestingRule`.

First of all, the class initializes the `FnTestingRule` harness, as explained
in [Testing Functions](../../docs/TestingFunctions.md).

[FunctionTest.java](src/test/java/com/fnproject/fn/examples/FunctionTest.java)
```java
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
```

Use `connectorHubTestFeature.givenEvent(event).enqueue();` to queue the request event 
and invoke the Function with `fn.thenRun(Function.class, "handler");`.

To verify the Subscription is working, check [Notification metrics](https://docs.oracle.com/en-us/iaas/Content/Notification/Tasks/view-chart-resource.htm#top)