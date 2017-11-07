package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.CompletionId;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fnproject.fn.runtime.flow.RemoteCompleterApiClient.*;

/**
 * Holder for a function-supplied value that contains an externalised representation of the references that that value contains
 */
abstract class Datum {

    public abstract Object asJavaValue(ClassLoader loader);

    public abstract void writeHeaders(HeaderWriter hw) throws IOException;

    public abstract void writeBody(OutputStream os) throws IOException;


    enum DatumType {
        blob(BlobDatum::readFromHttp),
        empty((x) -> new EmptyDatum()),
        stageref(StageRefDatum::readFromHttp),
        httpreq(HttpReqDatum::readFromHttp),
        httpresp(HttpRespDatum::readFromHttp);
        final DatumReader reader;

        DatumType(DatumReader reader) {
            this.reader = reader;
        }
    }

    interface DatumReader {
        Datum readDatum(org.apache.http.HttpResponse message) throws IOException;
    }

    void writePart(OutputStream os) throws IOException {
        writeHeaders(new HeaderWriter(os));
        os.write(new byte[]{'\r', '\n'});
        writeBody(os);
    }

    static class Blob {
        private final String contentType;
        private final byte[] data;

        Blob(String contentType, byte[] data) {
            this.contentType = contentType;
            this.data = data;
        }

        static Blob readFromHttp(org.apache.http.HttpResponse resp) throws IOException {
            String contentType = resp.getFirstHeader(CONTENT_TYPE_HEADER).getValue();
            byte[] data = IOUtils.toByteArray(resp.getEntity().getContent());
            return new Blob(contentType, data);
        }
    }

    public static class EmptyDatum extends Datum {
        @Override
        public Object asJavaValue(ClassLoader loader) {
            return null;
        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_EMPTY);

        }

        @Override
        public void writeBody(OutputStream os) {

        }

    }

    public static class StateDatum extends Datum {
        private final Flow.FlowState state;

        public StateDatum(Flow.FlowState state) {
            this.state = Objects.requireNonNull(state);
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            return state;
        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_STATE);

            String stateName;
            switch (state) {
                case SUCCEEDED:
                    stateName = "succeeded";
                    break;
                case FAILED:
                    stateName ="failed";
                    break;
                case CANCELLED:
                    stateName = "cancelled";
                    break;
                case KILLED:
                    stateName = "killed";
                    break;
                default:
                    throw new IllegalStateException("Unsupported state " + state);

            }
            hw.writeHeader(STATE_TYPE_HEADER,stateName);
        }

        @Override
        public void writeBody(OutputStream os) {

        }

    }


    public static class BlobDatum extends Datum {
        private final Blob data;

        BlobDatum(Blob data) {
            this.data = data;
        }


        static BlobDatum readFromHttp(org.apache.http.HttpResponse m) throws IOException {
            return new BlobDatum(Blob.readFromHttp(m));
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            Object o;
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data.data)) {
                    @Override
                    protected Class resolveClass(ObjectStreamClass classDesc)
                            throws IOException, ClassNotFoundException {

                        String cname = classDesc.getName();
                        try {
                            return loader.loadClass(cname);
                        } catch (ClassNotFoundException ex) {
                            return super.resolveClass(classDesc);
                        }
                    }
                };
                o = ois.readObject();
            } catch (Exception e) {
                throw new PlatformException("Failed to deserialize result", e);
            }

            return o;

        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB);
            hw.writeHeader(CONTENT_TYPE_HEADER, data.contentType);
        }

        @Override
        public void writeBody(OutputStream os) throws IOException {
            os.write(data.data);
        }
    }

    public enum ErrorType {
        unknown_error,
        stage_timeout,
        stage_failed,
        function_timeout,
        function_invoke_failed,
        stage_lost,
        invalid_stage_response
    }

    public static class ErrorDatum extends Datum {
        private final ErrorType type;
        private final String message;

        ErrorDatum(ErrorType type, String message) {
            this.type = Objects.requireNonNull(type);
            this.message = Objects.requireNonNull(message);
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            switch (type) {
                case stage_timeout:
                    return new StageTimeoutException(message);
                case stage_failed:
                    return new FlowCompletionException(new StageInvokeFailedException(message));
                case function_timeout:
                    return new FunctionTimeoutException(message);
                case function_invoke_failed:
                    return new FunctionInvokeFailedException(message);
                case stage_lost:
                    return new StageLostException(message);
                case invalid_stage_response:
                    return new InvalidStageResponseException(message);
                default:
                    return new PlatformException(message);
            }

        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_ERROR);
            hw.writeHeader(ERROR_TYPE_HEADER, type.name());
            hw.writeHeader(CONTENT_TYPE_HEADER, "text/plain; charset=utf-8");
        }

        @Override
        public void writeBody(OutputStream os) throws IOException {
            os.write(message.getBytes("UTF-8"));
        }
    }

    public static class HttpReqDatum extends Datum {
        private final HttpMethod method;
        private final Headers headers;
        private final byte[] body;

        HttpReqDatum(HttpMethod method, Headers headers, byte[] body) {
            this.method = Objects.requireNonNull(method);
            this.headers = Objects.requireNonNull(headers);
            this.body = body;
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            return new HttpRequest() {

                @Override
                public HttpMethod getMethod() {
                    return method;
                }

                @Override
                public Headers getHeaders() {
                    return headers;
                }

                @Override
                public byte[] getBodyAsBytes() {
                    return body;
                }

                @Override
                public String getBodyAsString() {
                    return new String(body);
                }
            };

        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ);
            hw.writeHeader(REQUEST_METHOD_HEADER, method.name());

            for (Map.Entry<String, String> e : headers.getAll().entrySet()) {
                hw.writeHeader(USER_HEADER_PREFIX + e.getKey(), e.getValue());
            }
        }

        @Override
        public void writeBody(OutputStream os) throws IOException {
            if (body != null) {
                os.write(body);
            }
        }

        static Datum readFromHttp(org.apache.http.HttpResponse basicHttpResponse) throws IOException {
            String methodStr = basicHttpResponse.getFirstHeader(METHOD_HEADER).getValue();

            HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());

            Map<String, String> headers = readHttpHeaders(basicHttpResponse);
            String contentType = basicHttpResponse.getFirstHeader(CONTENT_TYPE_HEADER).getValue();
            if (contentType != null) {
                headers.put(CONTENT_TYPE_HEADER, contentType);
            }

            byte[] body = IOUtils.toByteArray(basicHttpResponse.getEntity().getContent());
            return new HttpReqDatum(method, Headers.fromMap(headers), body);
        }
    }

    private static Map<String, String> readHttpHeaders(org.apache.http.HttpResponse basicHttpResponse) {
        Map<String, String> headers = new HashMap<>();
        for (Header h : basicHttpResponse.getAllHeaders()) {
            String hname = h.getName();
            if (hname.startsWith(USER_HEADER_PREFIX)) {
                String rheader = hname.substring(USER_HEADER_PREFIX.length());
                headers.put(rheader, h.getValue());
            }
        }
        return headers;
    }

    public static class HttpRespDatum extends Datum {
        private final int status;
        private final Headers headers;
        private final byte[] body;

        HttpRespDatum(int status, Headers headers, byte[] body) {
            this.status = status;
            this.headers = Objects.requireNonNull(headers);
            this.body = body;
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            return new HttpResponse() {
                @Override
                public int getStatusCode() {
                    return status;
                }

                @Override
                public Headers getHeaders() {
                    return headers;
                }

                @Override
                public byte[] getBodyAsBytes() {
                    return body;
                }

                @Override
                public String getBodyAsString() {
                    return new String(body);
                }
            };
        }

        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
            hw.writeHeader(RESULT_CODE_HEADER, String.valueOf(status));

            for (Map.Entry<String, String> e : headers.getAll().entrySet()) {
                hw.writeHeader(USER_HEADER_PREFIX + e.getKey(), e.getValue());
            }
        }

        @Override
        public void writeBody(OutputStream os) throws IOException {
            if (body != null) {
                os.write(body);
            }
        }

        static Datum readFromHttp(org.apache.http.HttpResponse basicHttpResponse) throws IOException {

            String status = basicHttpResponse.getFirstHeader(RESULT_CODE_HEADER).getValue();
            int code = Integer.parseInt(status);

            Map<String, String> headers = readHttpHeaders(basicHttpResponse);
            String contentType = basicHttpResponse.getFirstHeader(CONTENT_TYPE_HEADER).getValue();
            if (contentType != null) {
                headers.put(CONTENT_TYPE_HEADER, contentType);
            }

            byte[] body = IOUtils.toByteArray(basicHttpResponse.getEntity().getContent());
            return new HttpRespDatum(code, Headers.fromMap(headers), body);
        }

    }

    public static class StageRefDatum extends Datum {
        private final String stageId;

        StageRefDatum(String stageId) {
            this.stageId = stageId;
        }

        String getStageId() {
            return stageId;
        }

        @Override
        public Object asJavaValue(ClassLoader loader) {
            return new CompletionId(stageId);
        }


        @Override
        public void writeHeaders(HeaderWriter hw) throws IOException {
            hw.writeHeader(DATUM_TYPE_HEADER, DATUM_TYPE_STAGEREF);
            hw.writeHeader(STAGE_ID_HEADER, stageId);
        }

        @Override
        public void writeBody(OutputStream os) throws IOException {

        }

        static Datum readFromHttp(org.apache.http.HttpResponse basicHttpResponse) throws IOException {
            String stageRef = basicHttpResponse.getFirstHeader(STAGE_ID_HEADER).getValue();
            return new StageRefDatum(stageRef);
        }
    }

}

