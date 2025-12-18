package com.fnproject.events.testfns.apigatewayfns;

import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;

public class GrandChildGatewayTestFunction extends StringAPIGatewayTestFunction {
    @Override
    public APIGatewayResponseEvent<String> handler(APIGatewayRequestEvent<String> requestEvent) {
        return super.handler(requestEvent);
    }
}
