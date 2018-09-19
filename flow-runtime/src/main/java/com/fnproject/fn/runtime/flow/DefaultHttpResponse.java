package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.HttpResponse;

import java.util.Objects;

/**
 * Created on 27/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class DefaultHttpResponse implements HttpResponse {


    private final int statusCode;
    private final Headers headers;
    private final byte[] body;

    public DefaultHttpResponse(int statusCode, Headers headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = Objects.requireNonNull(headers);
        this.body = Objects.requireNonNull(body);
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public byte[] getBodyAsBytes() {
        return body;
    }
}
