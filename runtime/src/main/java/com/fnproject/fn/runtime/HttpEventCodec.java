package com.fnproject.fn.runtime;


import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.FunctionOutputHandlingException;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.*;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient.CONTENT_TYPE_HEADER;

/**
 * Reads input via an InputStream as an HTTP request.
 * <p>
 * This does not consume the whole event from the buffer,  The caller is responsible for ensuring that either   {@link InputEvent#consumeBody(Function)} or {@link InputEvent#close()}  is called before reading a new event
 */
class HttpEventCodec implements EventCodec {

    private final SessionInputBuffer sib;
    private final SessionOutputBuffer sob;
    private final HttpMessageParser<HttpRequest> parser;


    HttpEventCodec(InputStream input, OutputStream output) {

        SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        sib.bind(Objects.requireNonNull(input));
        this.sib = sib;

        SessionOutputBufferImpl sob = new SessionOutputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        sob.bind(output);
        this.sob = sob;

        parser = new DefaultHttpRequestParserFactory(null, null).create(sib, MessageConstraints.custom().setMaxHeaderCount(65535).setMaxLineLength(65535).build());
    }

    private static String requiredHeader(HttpRequest req, String id) {
        return Optional.ofNullable(req.getFirstHeader(id)).map(Header::getValue).orElseThrow(() -> new FunctionInputHandlingException("Incoming HTTP frame is missing required header: " + id));
    }

    @Override
    public final Optional<InputEvent> readEvent() {

        HttpRequest req;
        try {
            req = parser.parse();
        } catch (org.apache.http.ConnectionClosedException e) {
            // End of stream - signal normal termination
            return Optional.empty();
        } catch (IOException | HttpException e) {
            throw new FunctionInputHandlingException("Failed to read HTTP content from input", e);
        }

        InputStream bodyStream;
        if (req.getHeaders("content-length").length > 0) {
            long contentLength = Long.parseLong(requiredHeader(req, "content-length"));
            bodyStream = new ContentLengthInputStream(sib, contentLength);
        } else if (req.getHeaders("transfer-encoding").length > 0 &&
                req.getFirstHeader("transfer-encoding").getValue().equalsIgnoreCase("chunked")) {
            bodyStream = new ChunkedInputStream(sib);
        } else {
            bodyStream = new ByteArrayInputStream(new byte[]{});
        }
        String appName = requiredHeader(req, "app_name");
        String route = requiredHeader(req, "route");
        String method = requiredHeader(req, "method");
        String requestUrl = requiredHeader(req, "request_url");

        Map<String, String> headers = new HashMap<>();
        for (Header h : req.getAllHeaders()) {
            headers.put(h.getName(), h.getValue());
        }

        return Optional.of(new ReadOnceInputEvent(appName, route, requestUrl, method,
                bodyStream, Headers.fromMap(headers),
                QueryParametersParser.getParams(requestUrl)));

    }


    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public void writeEvent(OutputEvent evt) {
        try {
            // TODO: We buffer the whole output here just to get the content-length
            // TODO: functions should support chunked

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            evt.writeToOutput(bos);

            byte[] data = bos.toByteArray();

            BasicHttpResponse response;

            if (evt.isSuccess()) {
                response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "INVOKED"));
            } else {
                response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 500, "INVOKE FAILED"));
            }

            evt.getContentType().ifPresent((ct) -> response.setHeader(CONTENT_TYPE_HEADER, ct));
            response.setHeader("Content-length", String.valueOf(data.length));


            DefaultHttpResponseWriter writer = new DefaultHttpResponseWriter(sob);
            try {
                writer.write(response);
            } catch (HttpException e) {
                throw new FunctionOutputHandlingException("Failed to write response", e);
            }
            ContentLengthOutputStream clos = new ContentLengthOutputStream(sob, data.length);
            clos.write(data);
            clos.flush();
            clos.close();
            sob.flush();

        } catch (IOException e) {
            throw new FunctionOutputHandlingException("Failed to write output to stream", e);
        }
    }

}
