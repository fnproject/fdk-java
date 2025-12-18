package com.fnproject.events.input;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.QueryParameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIGatewayRequestEvent<T> {

    private final QueryParameters queryParameters;
    private final T body;
    private final String method;
    private final String requestUrl;
    private final Headers headers;

    public APIGatewayRequestEvent(QueryParameters queryParameters, T body, String method, String requestUrl, Headers headers) {
        this.queryParameters = queryParameters;
        this.body = body;
        this.method = method;
        this.requestUrl = requestUrl;
        this.headers = headers;
    }

    public T getBody() {
        return body;
    }

    public QueryParameters getQueryParameters() {
        return queryParameters;
    }

    public String getMethod() {
        return method;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        APIGatewayRequestEvent<?> that = (APIGatewayRequestEvent<?>) o;
        return Objects.equals(queryParameters, that.queryParameters) && Objects.equals(body, that.body) && Objects.equals(method, that.method) &&
            Objects.equals(requestUrl, that.requestUrl) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryParameters, body, method, requestUrl, headers);
    }
}