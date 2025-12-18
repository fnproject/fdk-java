package com.fnproject.events.output;

import com.fnproject.fn.api.Headers;

public class APIGatewayResponseEvent<T> {
    private final T body;
    private final Integer statusCode;
    private final Headers headers;

    private APIGatewayResponseEvent(T body, Integer statusCode, Headers headers) {
        this.headers = headers;
        this.body = body;
        this.statusCode = statusCode;
    }

    public static class Builder<T> {
        private T body;
        private Integer statusCode;
        private Headers headers;

        public Builder<T> body(T body) {
            this.body = body;
            return this;
        }

        public Builder<T> statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder<T> headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public APIGatewayResponseEvent<T> build() {
            return new APIGatewayResponseEvent<>(this.body, this.statusCode, this.headers);
        }
    }

    public Integer getStatus() {
        return statusCode;
    }

    public Headers getHeaders() {
        return headers;
    }

    public T getBody() {
        return body;
    }
}