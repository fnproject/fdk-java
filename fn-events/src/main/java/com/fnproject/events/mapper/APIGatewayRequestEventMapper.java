package com.fnproject.events.mapper;

import com.fnproject.events.input.APIGatewayRequestEvent;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl;

public class APIGatewayRequestEventMapper implements ApiGatewayRequestMapper {

    public <T> APIGatewayRequestEvent<T> toApiGatewayRequestEvent(HTTPGatewayContext context, T body) {
        QueryParameters queryParameters =
            context.getQueryParameters() != null ? context.getQueryParameters() : new QueryParametersImpl();
        Headers headers =
            context.getHeaders() != null ? context.getHeaders() : Headers.emptyHeaders();

        return new APIGatewayRequestEvent<>(queryParameters, body, context.getMethod(), context.getRequestURL(), headers);
    }
}
