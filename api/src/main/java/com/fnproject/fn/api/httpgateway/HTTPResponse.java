package com.fnproject.fn.api.httpgateway;

import com.fnproject.fn.api.Headers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created on 05/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface HTTPResponse {

    int getStatusCode();

    Headers getHeaders();

    /**
     * Write the body of the output to a stream
     *
     * @param out an outputstream to emit the body of the event
     * @throws IOException OutputStream exceptions percolate up through this method
     */
    void writeToOutput(OutputStream out) throws IOException;
}
