package com.fnproject.events;

import com.fnproject.events.coercion.APIGatewayCoercion;
import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.events.output.APIGatewayResponseEvent;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public abstract class APIGatewayFunction<T, U> {

    @FnConfiguration
    public void configure(RuntimeContext ctx){
        ctx.addInputCoercion(APIGatewayCoercion.instance());
        ctx.addOutputCoercion(APIGatewayCoercion.instance());
    }

    public abstract APIGatewayResponseEvent<U> handler(APIGatewayRequestEvent<T> requestEvent);
}
