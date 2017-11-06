package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.io.DefaultHttpRequestParserFactory;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.io.HttpMessageParser;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.fnproject.fn.runtime.flow.RemoteCompleterApiClient.*;

/**
 * Some general serialization utils ... hmmm
 */
final class SerUtils {

    interface Deserializer {
        /**
         * Construct a new ContentPart of the appropriate type.
         *
         * @param datumType    The datum-type field contents
         * @param entityReader The MIME entityReader for this item
         * @param bodyStream   An InputStream that contains the serialized contents
         * @return The ContentPart that corresponds to this item.
         * @throws IOException          If there is a problem reading the stream
         * @throws DeserializeException should a problem arise during deserialization
         */
        ContentPart deserialize(String datumType, EntityReader entityReader, InputStream bodyStream)
                throws DeserializeException, IOException, ClassNotFoundException;

        class DeserializeException extends Exception {
            DeserializeException() {
                super();
            }

            DeserializeException(String s) {
                super(s);
            }

            DeserializeException(Throwable t) {
                super(t);
            }

            DeserializeException(String s, Throwable t) {
                super(s, t);
            }
        }
    }

    /*
    FnProject-DatumType: blob
    FnProject-DatumType: empty
    FnProject-DatumType: error
    FnProject-DatumType: state
    FnProject-DatumType: stageref
    FnProject-DatumType: httpreq
    FnProject-DatumType: httpresp
    */
    private final static Map<String, Deserializer> deserializers = Collections.synchronizedMap(new HashMap<>());

    public static void registerDeserializer(String datumType, Deserializer ds) {
        deserializers.put(datumType, ds);
    }

    static {
        registerDeserializer(DATUM_TYPE_BLOB, (dt, h, is) -> {
            String contentType = h.getHeaderValue(CONTENT_TYPE_HEADER).orElseThrow(() -> new Deserializer.DeserializeException("Missing content-type"));
            if (!contentType.equalsIgnoreCase(CONTENT_TYPE_JAVA_OBJECT)) {
                throw new Deserializer.DeserializeException("Unexpected content-type: " + contentType);
            }
            try (ObjectInputStream ois = new ObjectInputStream(is)) {
                return new ContentPart(dt, contentType, ois.readObject());
            }
        });

        registerDeserializer(DATUM_TYPE_STAGEREF, (dt, h, is) -> {
            String stageId = h.getHeaderValue(STAGE_ID_HEADER)
                    .orElseThrow(() -> new Deserializer.DeserializeException("Missing stage ID header in stageref datum"));
            RemoteFlow.RemoteFlowFuture remote = ((RemoteFlow)Flows.currentFlow()).createRemoteFlowFuture(new CompletionId(stageId));
            return new ContentPart(dt, null, remote);
        });

        registerDeserializer(DATUM_TYPE_EMPTY, (dt, h, is) -> new ContentPart(dt, null, null));

        registerDeserializer(DATUM_TYPE_ERROR, (dt, h, is) -> {
            String resultStatus = h.getHeaderValue(RESULT_STATUS_HEADER).orElseThrow(() -> new Deserializer.DeserializeException("Missing FnProject-ResultStatus"));
            if (!resultStatus.equalsIgnoreCase(RESULT_STATUS_FAILURE)) {
                throw new Deserializer.DeserializeException("FnProject-ResultStatus expected to be failure");
            }
            String errorType = h.getHeaderValue(ERROR_TYPE_HEADER).orElseThrow(() -> new Deserializer.DeserializeException("Missing FnProject-ErrorType"));
            String contentType = h.getHeaderValue(CONTENT_TYPE_HEADER).orElse("text/plain");
            String errorMessage = IOUtils.toString(is, "utf-8");
            switch (errorType.toLowerCase()) {
                case ERROR_TYPE_STAGE_TIMEOUT:
                    return new ContentPart(dt, contentType, new StageTimeoutException(errorMessage));
                case ERROR_TYPE_STAGE_INVOKE_FAILED:
                    return new ContentPart(dt, contentType, new StageInvokeFailedException(errorMessage));
                case ERROR_TYPE_FUNCTION_TIMEOUT:
                    return new ContentPart(dt, contentType, new FunctionTimeoutException(errorMessage));
                case ERROR_TYPE_FUNCTION_INVOKE_FAILED:
                    return new ContentPart(dt, contentType, new FunctionInvokeFailedException(errorMessage));
                case ERROR_TYPE_STAGE_LOST:
                    return new ContentPart(dt, contentType, new StageLostException(errorMessage));
                case ERROR_TYPE_INVALID_STAGE_RESPONSE:
                    return new ContentPart(dt, contentType, new InvalidStageResponseException(errorMessage));
                default:
                    return new ContentPart(dt, contentType, new PlatformException(errorMessage));
            }
        });

        registerDeserializer(DATUM_TYPE_STATE, (dt, h, is) -> {
            String stateType = h.getHeaderValue(STATE_TYPE_HEADER).orElseThrow(() -> new Deserializer.DeserializeException("Missing state type header"));

            Flow.FlowState state;
            switch (stateType) {
                case "succeeded":
                    state = Flow.FlowState.SUCCEEDED;
                    break;
                case "failed":
                    state = Flow.FlowState.FAILED;
                    break;
                case "cancelled":
                    state = Flow.FlowState.CANCELLED;
                    break;
                case "killed":
                    state = Flow.FlowState.KILLED;
                    break;
                default:
                    state = Flow.FlowState.UNKNOWN;

            }
            return new ContentPart(dt, null, state);
        });

        registerDeserializer(DATUM_TYPE_HTTP_RESP, (dt, h, is) -> {
            int statusCode = h.getHeaderValue(RESULT_CODE_HEADER).map(Integer::parseInt)
                    .orElseThrow(() -> new Deserializer.DeserializeException(RESULT_CODE_HEADER + " mandatory field was not present on response from external function invocation"));

            Map<String, String> userHeaders = h.getHeaders().entrySet().stream()
                    .filter((entry) -> entry.getKey().toLowerCase().startsWith(USER_HEADER_PREFIX.toLowerCase()))
                    .collect(Collectors.toMap(
                            (entry) -> entry.getKey().substring(USER_HEADER_PREFIX.length()),
                            Map.Entry<String, String>::getValue
                    ));
            h.getHeaderValue(CONTENT_TYPE_HEADER)
                    .map((contentType) -> userHeaders.put(CONTENT_TYPE_HEADER, contentType));
            Headers headers = Headers.fromMap(userHeaders);
            byte[] body = IOUtils.toByteArray(is);

            String contentType = h.getHeaderValue(CONTENT_TYPE_HEADER).orElse(null);
            HttpResponse functionResponse = new HttpResponse() {
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
            };

            if (h.getHeaderValue(RESULT_STATUS_HEADER).orElse(RESULT_STATUS_SUCCESS).equalsIgnoreCase(RESULT_STATUS_FAILURE)) {
                return new ContentPart(dt, contentType, new FunctionInvocationException(functionResponse));
            } else {
                return new ContentPart(dt, contentType, functionResponse);
            }
        });

        registerDeserializer(DATUM_TYPE_HTTP_REQ, (dt, h, is) -> {
            Optional<String> methodName = h.getHeaderValue(REQUEST_METHOD_HEADER);
            try {
                HttpMethod method = methodName.map((m) -> HttpMethod.valueOf(m.toUpperCase()))
                        .orElseThrow(() -> new Deserializer.DeserializeException(REQUEST_METHOD_HEADER + " mandatory field was not present on response from external completion"));
                Map<String, String> userHeaders = h.getHeaders().entrySet().stream()
                        .filter((entry) -> entry.getKey().toLowerCase().startsWith(USER_HEADER_PREFIX.toLowerCase()))
                        .collect(Collectors.toMap(
                                (entry) -> entry.getKey().substring(USER_HEADER_PREFIX.length()),
                                Map.Entry::getValue
                        ));
                h.getHeaderValue(CONTENT_TYPE_HEADER)
                        .map((contentType) -> userHeaders.put(CONTENT_TYPE_HEADER, contentType));
                Headers headers = Headers.fromMap(userHeaders);

                byte[] body = IOUtils.toByteArray(is);

                String contentType = h.getHeaderValue(CONTENT_TYPE_HEADER).orElse(null);

                com.fnproject.fn.api.flow.HttpRequest req = new com.fnproject.fn.api.flow.HttpRequest() {
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
                };

                if (h.getHeaderValue(RESULT_STATUS_HEADER).orElse(RESULT_STATUS_SUCCESS).equalsIgnoreCase(RESULT_STATUS_FAILURE)) {
                    return new ContentPart(dt, contentType, new ExternalCompletionException(req));
                } else {
                    return new ContentPart(dt, contentType, req);
                }
            } catch (IllegalArgumentException e) {
                throw new Deserializer.DeserializeException(REQUEST_METHOD_HEADER + " had unrecognised value: " + methodName.orElse("(missing)"));
            }

        });
    }


    static class ContentPart {
        private final String datumType;
        private final String contentType;
        private final Object content;

        static ContentPart readFromStream(EntityReader entityReader)
                throws IOException, Deserializer.DeserializeException, ClassNotFoundException {
            String datumType = entityReader.getHeaderValue(DATUM_TYPE_HEADER).orElseThrow(() -> new Deserializer.DeserializeException("Missing Datum Type"));
            Deserializer deserializer = deserializers.get(datumType);
            if (deserializer == null) {
                throw new Deserializer.DeserializeException("Unknown Datum-Type: " + datumType + ", could not find deserializer");
            }
            return deserializer.deserialize(datumType, entityReader, entityReader.getContentStream());
        }

        ContentPart(String datumType, String contentType, Object content) {
            this.datumType = datumType;
            this.contentType = contentType;
            this.content = content;
        }

        String getDatumType() {
            return datumType;
        }

        String getContentType() {
            return contentType;
        }

        Object get() {
            return content;
        }
    }


    static class ContentStream {
        private final BoundaryInputStream bis;

        public ContentStream(String contentType, InputStream is) throws IOException {
            String boundary = getBoundary(contentType);
            bis = new BoundaryInputStream(new BufferedInputStream(is), boundary);

            // Skip the preamble
            IOUtils.copy(bis.getCurrentStream(), NullOutputStream.NULL_OUTPUT_STREAM);
        }

        static private String getBoundary(String contentType) {
            ContentType type = ContentType.parse(contentType);
            return type.getParameter("boundary");
        }

        public boolean hasMore() {
            return !bis.atEndMarker();
        }

        public Object readObject() throws Deserializer.DeserializeException, IOException, ClassNotFoundException {
            return readContentPart().get();
        }

        public Object readObject(String expectedDatumType) throws Deserializer.DeserializeException, IOException, ClassNotFoundException {
            ContentPart part = readContentPart();
            if (expectedDatumType.equalsIgnoreCase(part.getDatumType())) {
                return part.get();
            }
            throw new Deserializer.DeserializeException("Unexpected FnProject-DatumType: wanted " + expectedDatumType + ", received: " + part.getDatumType());
        }

        public ContentPart readContentPart() throws IOException, Deserializer.DeserializeException, ClassNotFoundException {
            if (bis.atEndMarker()) {
                throw new EOFException();
            }
            // Read headers from BIS
            EntityReader er = new MultipartReader(bis.getCurrentStream());

            // Read body
            return ContentPart.readFromStream(er);
        }

        public void close() throws IOException {
            bis.close();
        }

    }

    static class BoundaryInputStream implements Closeable {
        private final BufferedInputStream is;
        private final byte[] boundary;
        private InputStream currentStream = null;
        private boolean allDone = false;

        public BoundaryInputStream(BufferedInputStream is, String boundary) throws UnsupportedEncodingException {
            this.is = is;
            boundary = "\r\n--" + boundary;
            this.boundary = boundary.getBytes("US-ASCII");
        }

        public InputStream getCurrentStream() {
            if (allDone) {
                return null;
            }
            if (currentStream == null) {
                currentStream = new UpToBoundaryInputStream();
            }
            return currentStream;
        }

        public boolean atEndMarker() {
            return allDone;
        }

        @Override
        public void close() throws IOException {
            is.close();
        }

        public class UpToBoundaryInputStream extends InputStream implements Closeable {
            private boolean atEof = false;
            private boolean atBeginning = true;
            // n = 0.. : read n bytes of the sought-for marker, \r\n--{boundary bytes}

            // \r\n

            UpToBoundaryInputStream() {
            }

            @Override
            public int read() throws IOException {
                if (atEof) {
                    return -1;
                }
                int firstChar = is.read();

                if (firstChar == -1) {
                    atEof = true;
                    currentStream = null;
                    return -1;
                }

                if (!atBeginning && firstChar != '\r') {
                    atBeginning = false;
                    return firstChar;
                }

                is.mark(65536);
                int found;

                if (firstChar != '\r' && atBeginning) {
                    found = 2;
                } else {
                    found = 0;
                }

                int c = firstChar;
                while (found < boundary.length && c == boundary[found]) {
                    found++;
                    c = is.read();
                }

                if (found != boundary.length) {
                    is.reset();
                    atBeginning = false;
                    return firstChar;
                }

                // We've matched (\r\n)?--boundary
                if (c == '\r' && is.read() == '\n') {
                    atEof = true;
                    currentStream = null;
                    return -1;
                } else if (c == '-' && is.read() == '-' && is.read() == '\r' && is.read() == '\n') {
                    atEof = true;
                    allDone = true;
                    currentStream = null;
                    return -1;
                }

                is.reset();
                atBeginning = false;
                return firstChar;
            }

            public void close() throws IOException {
                IOUtils.copy(this, NullOutputStream.NULL_OUTPUT_STREAM);
            }
        }
    }

    /**
     * Wrap up the handling of an InputStream -> set of headers + content.
     */
    static class MultipartReader implements EntityReader {
        private HttpRequest req = null;
        SessionInputBufferImpl sib = null;

        public MultipartReader(InputStream is) {
            sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
            // This is a bit crap; dig into Apache httpcomponents for better parsing
            ByteArrayInputStream firstLine = new ByteArrayInputStream("POST / HTTP/1.1\r\n".getBytes());

            sib.bind(new SequenceInputStream(firstLine, Objects.requireNonNull(is)));

            HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParserFactory(null, null)
                    .create(sib, MessageConstraints.custom().setMaxHeaderCount(65535).setMaxLineLength(65535).build());

            try {
                req = parser.parse();
            } catch (org.apache.http.ConnectionClosedException e) {
                // End of stream - signal normal termination
                return;
            } catch (IOException | HttpException e) {
                throw new FunctionInputHandlingException("Failed to read HTTP content from input", e);
            }
        }

        private Header getHeader(String header) {
            return req.getFirstHeader(header);
        }

        @Override
        public String getHeaderElement(String h, String e) {
            return getHeader(h).getElements()[0].getParameterByName(e).getValue();
        }

        @Override
        public Optional<String> getHeaderValue(String header) {
            Header h = getHeader(header);
            if (h == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(h.getValue());
        }

        @Override
        public InputStream getContentStream() {
            return new IdentityInputStream(sib);
        }

        @Override
        public Map<String, String> getHeaders() {
            return Arrays.stream(req.getAllHeaders()).collect(Collectors.toMap(
                    Header::getName,
                    Header::getValue,
                    (a, b) -> a + ", " + b
            ));
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(512)) {
            try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(obj);
                return baos.toByteArray();
            }
        }
    }

    static Object deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }

}
