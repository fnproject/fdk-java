package com.fnproject.events.mapper;

import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

public interface ApiGatewayRequestMapper {
    <T> APIGatewayRequestEvent<T> toApiGatewayRequestEvent(HTTPGatewayContext context, T body);
}
