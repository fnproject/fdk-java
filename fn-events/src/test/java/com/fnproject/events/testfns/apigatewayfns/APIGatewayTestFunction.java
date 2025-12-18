package com.fnproject.events.testfns.apigatewayfns;

import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.events.testfns.Animal;
import com.fnproject.events.testfns.Car;

public class APIGatewayTestFunction extends APIGatewayFunction<Animal, Car> {
    @Override
    public APIGatewayResponseEvent<Car> handler(APIGatewayRequestEvent<Animal> requestEvent) {
        return new APIGatewayResponseEvent.Builder<Car>()
            .body(new Car("ford", 4))
            .build();
    }
}
