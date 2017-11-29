package com.fnproject.fn.runtime.flow;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class HttpClient {

    private static final int REQUEST_TIMEOUT_MS = 600000;
    private static final int CONNECT_TIMEOUT_MS = 1000;

    public HttpResponse execute(HttpRequest request) throws IOException {

        request.headers.putIfAbsent("Content-Type", "application/octet-stream");
        request.headers.putIfAbsent("Accept", "application/octet-stream");

        URLConnection connection = request.toUrl().openConnection();

        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MS);

        for (Map.Entry<String, String> requestHeader : request.headers.entrySet()) {
            connection.setRequestProperty(requestHeader.getKey(), requestHeader.getValue());
        }

        if (request.method.equals("POST")) {
            connection.setDoOutput(true);  // implies POST
            connection.getOutputStream().write(request.bodyBytes);
        }

        connection.connect();


        // DEAL WITH THE RESPONSE

        HttpURLConnection httpConn = (HttpURLConnection) connection;

        int status = httpConn.getResponseCode();
        HttpResponse response = new HttpResponse(status);

        for (Map.Entry<String, List<String>> header : httpConn.getHeaderFields().entrySet()) {
            if (header.getKey() == null) {
                response.setStatusLine(header.getValue().get(0));
            } else {
                response.addHeader(header.getKey(), header.getValue().get(0));
            }
        }

        if (200 <= status && status < 300) {
            response.setEntity(httpConn.getInputStream());
        } else {
            response.setEntity(httpConn.getErrorStream());
        }

        return response;

    }

    public static class HttpResponse implements EntityReader, Closeable {
        final int status;
        final Map<String, String> headers = new HashMap<>();
        String statusLine;
        InputStream body = new ByteArrayInputStream(new byte[0]);

        public HttpResponse(int status) {
            this.status = status;
        }

        public int getStatusCode() {
            return status;
        }

        public HttpResponse setStatusLine(String statusLine) {
            this.statusLine = statusLine;
            return this;
        }

        public HttpResponse addHeader(String name, String value) {
            headers.put(name.toLowerCase(), value);
            return this;
        }

        public HttpResponse setEntity(InputStream body) {
            this.body = body;
            return this;
        }

        public String getHeader(String headerName) {
            return headers.get(headerName.toLowerCase());
        }

        @Override
        public String getHeaderElement(String h, String e) {
            // This is ugly: we need to parse a header element here to recover the pieces
            return null;
        }

        @Override
        public Optional<String> getHeaderValue(String header) {
            return Optional.ofNullable(getHeader(header.toLowerCase()));
        }

        @Override
        public InputStream getContentStream() {
            return body;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        public String entityAsString() throws IOException {
            return body == null ? null : IOUtils.toString(body, StandardCharsets.UTF_8);
        }

        public InputStream getEntity() {
            return body;
        }

        public byte[] entityAsBytes() throws IOException {
            return body == null ? null : IOUtils.toByteArray(body);
        }

        @Override
        public String toString() {
            return "HttpResponse{" +
                    "status=" + status +
                    ", statusLine='" + statusLine + '\'' +
                    ", headers=" + headers +
                    '}';
        }


        @Override
        public void close() throws IOException {
            if (body != null) {
                body.close();
            }
        }
    }


    public static class HttpRequest {
        final String method;
        final String url;
        final Map<String, String> query = new HashMap<>();
        byte[] bodyBytes = new byte[0];
        final Map<String, String> headers = new HashMap<>();

        private HttpRequest(String method, String url) {
            this.method = method;
            this.url = url;

        }

        public HttpRequest withQueryParam(String key, String value) {
            query.put(key, value);
            return this;
        }

        public HttpRequest withBody(byte[] bodyBytes) {
            this.bodyBytes = bodyBytes;
            return this;
        }

        public HttpRequest withHeader(String header, String value) {
            headers.put(header, value);
            return this;
        }

        private String enc(String s) {
            try {
                return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 is not a supported encoding on this platform. Where are we?", e);
            }
        }

        private URL toUrl() throws IOException {
            String queryParams = query.entrySet().stream().map((kv) -> enc(kv.getKey()) + "=" + enc(kv.getValue())).collect(Collectors.joining("&"));
            URL url = new URL(this.url + (queryParams.isEmpty() ? "" : "?" + queryParams));
            return url;
        }

        public HttpRequest withAdditionalHeaders(Map<String, String> additionalHeaders) {
            this.headers.putAll(additionalHeaders);
            return this;
        }
    }

    public static HttpRequest prepareGet(String url) {
        return new HttpRequest("GET", url);
    }

    public static HttpRequest preparePost(String url) {
        return new HttpRequest("POST", url);
    }


}
