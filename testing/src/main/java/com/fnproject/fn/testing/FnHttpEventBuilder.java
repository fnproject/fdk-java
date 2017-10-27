package com.fnproject.fn.testing;

import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

class FnHttpEventBuilder {
    private Map<String, List<String>> queryParams = new TreeMap<>();
    private boolean streamRead = false;
    private String callId;
    private String method;
    private String appName;
    private String route;
    private String requestUrl;
    private byte[] bodyBytes = new byte[0];
    private InputStream bodyStream;
    private int contentLength = 0;
    private Map<String, String> headers = new HashMap<>();


    public FnHttpEventBuilder withHeader(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        headers.put(key, value);
        return this;
    }

    public FnHttpEventBuilder withBody(InputStream body, int contentLength) {
        Objects.requireNonNull(body, "body");
        if (contentLength < 0) {
            throw new IllegalArgumentException("Invalid contentLength");
        }
        // This is for safety. Because we concatenate events, an input stream shorter than content length will cause
        // the implementation to continue reading through to the next http request. We need to avoid a sort of
        // buffer overrun.
        // FIXME: Make InputStream handling simpler.
        SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        sib.bind(body);
        this.bodyStream = new ContentLengthInputStream(sib, contentLength);
        this.contentLength = contentLength;
        return this;
    }

    public FnHttpEventBuilder withBody(byte[] body) {
        Objects.requireNonNull(body, "body");
        this.bodyBytes = body;
        this.contentLength = body.length;
        this.bodyStream = null;
        return this;
    }

    public FnHttpEventBuilder withBody(String body) {
        byte stringAsBytes[] = Objects.requireNonNull(body, "body").getBytes();
        return withBody(stringAsBytes);
    }

    public FnHttpEventBuilder withRoute(String route) {
        Objects.requireNonNull(route, "route");
        this.route = route;
        return this;
    }

    public FnHttpEventBuilder withMethod(String method) {
        Objects.requireNonNull(method, "method");
        this.method = method.toUpperCase();
        return this;
    }

    public FnHttpEventBuilder withAppName(String appName) {
        Objects.requireNonNull(appName, "appName");
        this.appName = appName;
        return this;
    }

    public FnHttpEventBuilder withRequestUrl(String requestUrl) {
        Objects.requireNonNull(requestUrl, "requestUrl");
        this.requestUrl = requestUrl;
        return this;
    }

    public FnHttpEventBuilder withCallId(String callId){
        Objects.requireNonNull(callId, "callId");
        this.callId = callId;
        return this;
    }

    private String buildQueryParams() {
        return queryParams.entrySet().stream()
                .flatMap((e) -> e.getValue().stream()
                        .map((v) -> urlEncode(e.getKey()) + "=" + urlEncode(v)))
                .collect(Collectors.joining("&"));
    }


    public FnHttpEventBuilder withQueryParameter(String key, String value) {
        if (!this.queryParams.containsKey(key)) {
            this.queryParams.put(key, new ArrayList<>());
        }
        this.queryParams.get(key).add(value);
        return this;
    }

    public FnHttpEventBuilder withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Your jvm doesn't support UTF-8, cannot continue.");
        }
    }

    private InputStream bodyAsStream() {
        if (bodyStream != null) {
            if (streamRead) {
                throw new IllegalStateException("events with an overridden input stream can only be enqueued once");
            }
            streamRead = true;
            return bodyStream;
        } else {
            return new ByteArrayInputStream(bodyBytes);
        }
    }

    private void verify() {
        Objects.requireNonNull(method, "method not set");
        Objects.requireNonNull(appName, "appName not set");
        Objects.requireNonNull(route, "route not set");
        Objects.requireNonNull(requestUrl, "requestUrl not set");

    }

    public Map<String, String> currentEventEnv() {
        verify();
        Map<String, String> env = new HashMap<>();
        env.put("FN_APP_NAME", appName);
        env.put("FN_PATH", route);
        return env;
    }

    public InputStream currentEventInputStream() {
        verify();

        String queryParamsFullString = buildQueryParams();
        StringBuilder inputString = new StringBuilder();

        inputString.append(method);
        inputString.append(" / HTTP/1.1\r\n");
        inputString.append("Fn_Method: ").append(method).append("\r\n");
        inputString.append("Fn_Call_Id: ").append(callId).append("\r\n");
        inputString.append("Fn_Request_url: ").append(requestUrl);

        if (!queryParamsFullString.isEmpty()) {
            inputString.append("?").append(queryParamsFullString);
        }
        inputString.append("\r\n");


        inputString.append("Content-length: ").append(Integer.toString(contentLength)).append("\r\n");

        headers.forEach((k, v) -> inputString.append(k).append(": ").append(String.join(", ", v)).append("\r\n"));


        inputString.append("\r\n");

        return new SequenceInputStream(
                new ByteArrayInputStream(inputString.toString().getBytes()),
                bodyAsStream());
    }


}
