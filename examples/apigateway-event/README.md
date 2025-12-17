# Example Fn Java FDK : API Gateway

This example provides a Function to use as an API Gateway backend.
The function accepts a typed request for easy object handling and returns
http response.

## Dependencies

* [fn-events] for APIGatewayFunction classes.
* [fn-events-testing] for APIGatewayFunction testing library.

## Demonstrated FDK features

This example showcases how to use the fn-event APIGatewayFunction to 
use a Function as the backend.

## Step by step

Set the API Gateway and Function 
backend [Adding a Function in OCI Functions as an API Gateway Back End](https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayusingfunctionsbackend.htm)

The Function entrypoint extends the `APIGatewayFunction` abstract class.
Note: the [func.yaml](func.yaml) entrypoint remains the class which extends `APIGatewayFunction` 
e.g. `cmd: com.fnproject.fn.examples.Function::handler`


[Function.java](src/main/java/com/fnproject/fn/examples/Function.java)
```java
import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.Headers;
import org.apache.http.HttpStatus;

public class Function extends APIGatewayFunction<RequestEmployee, ResponseEmployee> {

    @Override
    public APIGatewayResponseEvent<ResponseEmployee> handler(APIGatewayRequestEvent<RequestEmployee> requestEvent) {
        ResponseEmployee employee = new ResponseEmployee();
        Optional<String> id = requestEvent.getQueryParameters().get("id");
        id.ifPresent(s -> employee.setId(Integer.parseInt(s)));

        if (requestEvent.getBody() != null) {
            employee.setName(requestEvent.getBody().getName());
        }

        return new APIGatewayResponseEvent.Builder<ResponseEmployee>()
            .statusCode(HttpStatus.SC_CREATED)
            .headers(Headers.emptyHeaders()
                .addHeader("X-Custom-Header", "HeaderValue")
                .addHeader("X-Custom-Header-2", "HeaderValue2"))
            .body(employee)
            .build();
    }
}
```
The APIGatewayRequestEvent.class `requestUrl` is relative to the deployment 
path prefix [see API Gateway using HTTP backend](https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayusinghttpbackend.htm#usingjson)


The class [RequestEmployee.java](src/main/java/com/fnproject/fn/examples/RequestEmployee.java) is the request body type and 
[ResponseEmployee.java](src/main/java/com/fnproject/fn/examples/ResponseEmployee.java) is the response body type. 
These are passed in position 1 and 2 of abstract class
[Function.java](src/main/java/com/fnproject/fn/examples/Function.java) 
`public class Function extends APIGatewayFunction<RequestEmployee, ResponseEmployee> {`.

To return an error response, throw RuntimeException.class.

## Test walkthrough

Unit testing `APIGatewayFunction` is supported with the `APIGatewayTestFeature` and `FnTestingRule`.

First of all, the class initializes the `FnTestingRule` harness, as explained
in [Testing Functions](../../docs/TestingFunctions.md).

[FunctionTest.java](src/test/java/com/fnproject/fn/examples/FunctionTest.java)
```java
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.events.testing.APIGatewayTestFeature;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class FunctionTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    private final APIGatewayTestFeature apiGatewayFeature = APIGatewayTestFeature.createDefault(fn);

    @Test
    public void testHttpAttributes() throws IOException {
        RequestEmployee requestEmployee = new RequestEmployee();
        requestEmployee.setName("John");

        APIGatewayRequestEvent<RequestEmployee> event = mock(APIGatewayRequestEvent.class);

        when(event.getBody()).thenReturn(requestEmployee);
        when(event.getMethod()).thenReturn("POST");
        when(event.getRequestUrl()).thenReturn("/v2?id=123");
        when(event.getQueryParameters()).thenReturn(new QueryParametersImpl(Collections.singletonMap("id", Collections.singletonList("123"))));
        when(event.getHeaders()).thenReturn(Collections.unmodifiableMap(new HashMap<String, List<String>>() {{
            put("myHeader", Collections.singletonList("headerValue"));
        }}));

        apiGatewayFeature.givenEvent(event)
            .enqueue();

        fn.thenRun(Function.class, "handler");

        APIGatewayResponseEvent<ResponseEmployee> responseEvent = apiGatewayFeature.getResult(ResponseEmployee.class);

        ResponseEmployee responseEventBody = responseEvent.getBody();
        assertEquals(123, responseEventBody.getId());
        assertEquals("John", responseEventBody.getName());
        assertEquals(Integer.valueOf(201), responseEvent.getStatus());
        assertEquals("HeaderValue", responseEvent.getHeaders().get("X-Custom-Header").get(0));
        assertEquals("HeaderValue2", responseEvent.getHeaders().get("X-Custom-Header-2").get(0));
    }
}
```

Use `apiGatewayFeature.givenEvent(event).enqueue();` to queue the request event 
and invoke the Function with `fn.thenRun(Function.class, "handler");`.

And get the Function response using 
`APIGatewayResponseEvent<ResponseEmployee> responseEvent = apiGatewayFeature.getResult(ResponseEmployee.class);`
