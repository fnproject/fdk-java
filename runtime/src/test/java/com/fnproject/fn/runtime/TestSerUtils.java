package com.fnproject.fn.runtime;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.message.BasicHeader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.fnproject.fn.runtime.flow.RemoteCompleterApiClient.*;

public class TestSerUtils {
    public static byte[] serializeToBytes(Object o) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(o);
                out.close();
                return baos.toByteArray();
            }
        }
    }

    public static byte[] concat(byte[]... inputs) {
        int len = Arrays.stream(inputs).map((a) -> a.length).reduce(0, Integer::sum);
        byte[] newArray = new byte[len];
        int offset = 0;
        for (byte[] input : inputs) {
            System.arraycopy(input, 0, newArray, offset, input.length);
            offset += input.length;
        }
        return newArray;
    }

    public static class HttpMultipartSerialization {
        private final List<Header> headers = new ArrayList<>();
        private final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        private int argIndex = -1;
        private boolean built = false;
        private HttpEntity entity = null;

        public HttpMultipartSerialization(Object... parts) throws IOException {
            for (Object obj: parts) {
                addJavaEntity(obj);
            }
        }

        public HttpMultipartSerialization addHeader(String name, String value) {
            if (built) {
                throw new IllegalStateException("Cannot modify entity after building");
            }
            headers.add(new BasicHeader(name, value));
            return this;
        }

        public HttpMultipartSerialization addEntity(String contentType, byte[] content, Map<String, String> otherHeaders) throws IOException {
            if (built) {
                throw new IllegalStateException("Cannot modify entity after building");
            }
            return addEntity(argIndex < 0 ? "closure" : "arg_" + argIndex, contentType, content, otherHeaders);
        }

        public HttpMultipartSerialization addEntity(String name, String contentType, byte[] content, Map<String, String> otherHeaders) throws IOException {
            if (built) {
                throw new IllegalStateException("Cannot modify entity after building");
            }
            argIndex ++;
            FormBodyPartBuilder fbp = FormBodyPartBuilder.create(name, new ByteArrayBody(content, ContentType.create(contentType), null));
            otherHeaders.forEach(fbp::addField);
            entityBuilder.addPart(fbp.build());
            return this;
        }

        public HttpMultipartSerialization addJavaEntity(Object obj) throws IOException {
            return addEntity(CONTENT_TYPE_JAVA_OBJECT, serializeToBytes(obj), Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB));
        }

        public HttpMultipartSerialization addEmptyEntity() throws IOException {
            return addEntity("application/void", new byte[]{}, Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_EMPTY));
        }

        public HttpMultipartSerialization addFnStageRefEntity(String stageId) throws IOException {
            Map<String, String> hs = new HashMap<>();
            hs.put(DATUM_TYPE_HEADER, DATUM_TYPE_STAGEREF);
            hs.put(STAGE_ID_HEADER, stageId);
            return addEntity("text/plain", new byte[]{}, hs);
        }

        public HttpMultipartSerialization addErrorEntity(String errorType) throws IOException {
            Map<String, String> hs = new HashMap<>();
            hs.put(DATUM_TYPE_HEADER, DATUM_TYPE_ERROR);
            hs.put(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
            hs.put(ERROR_TYPE_HEADER, errorType);
            return addEntity("text/plain", ("A platform error with the type " + errorType).getBytes(), hs);
        }

        public HttpMultipartSerialization addFnResultEntity(int resultCode, Map<String, String> customHeaders, String contentType, String body) throws IOException {
            Map<String, String> hs = new HashMap<>();
            hs.putAll(customHeaders);
            hs.put(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
            hs.put(RESULT_CODE_HEADER, Integer.toString(resultCode));

            return addEntity(contentType, body.getBytes(), hs);
        }


        public Map<String, String> getHeaders() {
            build();
            return headers.stream().collect(Collectors.toMap(Header::getName, Header::getValue));
        }

        public InputStream getContentStream() throws IOException {
            build();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            entity.writeTo(os);
            os.flush();
            return new ByteArrayInputStream(os.toByteArray());
        }

        private void build() {
            if (built) {
                return;
            }
            entity = entityBuilder.build();
            headers.add(entity.getContentType());
            long len = entity.getContentLength();
            if (len >= 0) {
                addHeader("Content-Length", Long.toString(len));
            }
            built = true;
        }
    }

}
