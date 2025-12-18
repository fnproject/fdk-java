package com.fnproject.events.testfns.apigatewayfns;

import com.fnproject.events.APIGatewayFunction;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;

public class StringAPIGatewayTestFunction extends APIGatewayFunction<String, String> {
    @Override
    public APIGatewayResponseEvent<String> handler(APIGatewayRequestEvent<String> requestEvent) {
        return new APIGatewayResponseEvent.Builder<String>()
            .body("test response")
            .statusCode(200)
            .headers(requestEvent.getHeaders())
            .build();
    }
}
