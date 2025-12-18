package com.fnproject.events.testfns.apigatewayfns;

import java.util.Collections;
import java.util.List;
import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.Car;

public class ListAPIGatewayTestFunction extends APIGatewayFunction<List<Animal>, List<Car>> {
    @Override
    public APIGatewayResponseEvent<List<Car>> handler(APIGatewayRequestEvent<List<Animal>> requestEvent) {
        return new APIGatewayResponseEvent.Builder<List<Car>>()
            .body(Collections.singletonList(new Car("ford", 4)))
            .build();
    }
}