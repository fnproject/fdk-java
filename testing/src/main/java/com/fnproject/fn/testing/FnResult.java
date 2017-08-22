package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.runtime.exception.FunctionOutputHandlingException;
import org.apache.http.HttpException;
import org.apache.http.ProtocolVersion;
import org.apache.http.impl.io.ContentLengthOutputStream;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A simple abstraction for a parsed HTTP response returned by a function
 */
public interface FnResult {
    /**
     * Returns the body of the function result as a byte array
     *
     * @return the function response body
     */
    byte[] getBodyAsBytes();

    /**
     * Returns the body of the function response as a string
     *
     * @return a function response body
     */
    String getBodyAsString();

    /**
     * A map of the headers returned by the function
     * <p>
     * These are squashed so duplicated headers will be ignored (takes the first header).
     *
     * @return a map of headers
     */
    Headers getHeaders();

    /**
     * Returns the HTTP status code of the function response
     *
     * @return the HTTP status code returned by the function
     */
    int getStatus();

    /**
     * Returns the entire raw HTTP response as a byte array
     *
     * @return the raw HTTP response bytes
     */
    default byte[] getHttpResponse() {
        byte[] data = getBodyAsBytes();

        BasicHttpResponse response;

        response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), getStatus(), "Function invoked"));

        response.setHeader("Content-type", "application/octet-stream");
        response.setHeader("Content-length", String.valueOf(data.length));

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        SessionOutputBufferImpl sob = new SessionOutputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        sob.bind(byteos);
        DefaultHttpResponseWriter writer = new DefaultHttpResponseWriter(sob);
        try {
            writer.write(response);
            ContentLengthOutputStream clos = new ContentLengthOutputStream(sob, data.length);
            clos.write(data);
            clos.flush();
            clos.close();
            sob.flush();
        } catch (HttpException | IOException e) {
            throw new FunctionOutputHandlingException("Failed to write response", e);
        }
        return byteos.toByteArray();
    }
}
