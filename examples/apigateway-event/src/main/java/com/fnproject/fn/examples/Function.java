package com.fnproject.fn.examples;

import java.util.Optional;
import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.Headers;
import org.apache.http.HttpStatus;


public class Function extends APIGatewayFunction<RequestEmployee, ResponseEmployee> {

    private final EmployeeService employeeService;

    public Function() {
        this.employeeService = new EmployeeService();
    }

    @Override
    public APIGatewayResponseEvent<ResponseEmployee> handler(APIGatewayRequestEvent<RequestEmployee> requestEvent) {
        Optional<String> id = requestEvent.getQueryParameters().get("id");
        RequestEmployee requestEmployee = requestEvent.getBody();

        ResponseEmployee responseEmployee = employeeService.createEmployee(requestEmployee, id);

        return new APIGatewayResponseEvent.Builder<ResponseEmployee>()
            .statusCode(HttpStatus.SC_CREATED)
            .headers(Headers.emptyHeaders()
                .addHeader("X-Custom-Header", "HeaderValue")
                .addHeader("X-Custom-Header-2", "HeaderValue2"))
            .body(responseEmployee)
            .build();
    }
}
