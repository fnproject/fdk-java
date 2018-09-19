package com.fnproject.fn.api.httpgateway;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.QueryParameters;

import java.io.InputStream;
import java.util.function.Function;

/**
 * Created on 05/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface HTTPGatewayRequest {

    String getMethod();

    String getRequestURI();

    Headers getHeaders();

    QueryParameters getQueryParameters();

    <T> T consumeBody(Function<InputStream, T> dest);
}
