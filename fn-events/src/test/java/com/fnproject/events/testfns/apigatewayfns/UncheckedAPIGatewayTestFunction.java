package com.fnproject.events.testfns.apigatewayfns;

import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;

public class UncheckedAPIGatewayTestFunction extends APIGatewayFunction {
    @Override
    public APIGatewayResponseEvent handler(APIGatewayRequestEvent requestEvent) {
        return new APIGatewayResponseEvent.Builder()
            .body("test response")
            .build();
    }
}
