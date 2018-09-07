package com.fnproject.fn.api.flow;

/**
 * Enum representing the different HTTP types that can be used to invoke an external function
 *
 * @see Flow#invokeFunction
 */
public enum HttpMethod {
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH");

    private final String verb;

    HttpMethod(String verb) {
        this.verb = verb;
    }

    @Override
    public String toString() {
        return this.verb;
    }
}
