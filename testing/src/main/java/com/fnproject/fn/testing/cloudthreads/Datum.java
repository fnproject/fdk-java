package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.testing.FnResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;

/**
 * Holder  for a function-supplied value that contains an externalised representation of the references that that value contains
 * Created by OCliffe on 08/05/2017.
 */
public abstract class Datum {

    public abstract Object toJava(boolean success);

    public static class Blob {
        private final String contentType;
        private final byte[] data;

        public Blob(String contentType, byte[] data) {
            this.contentType = contentType;
            this.data = data;
        }
    }

    public static class EmptyDatum extends Datum {

        @Override
        public Object toJava(boolean success) {
            return null;
        }
    }


    public static class BlobDatum extends Datum {
        private final Blob data;

        private BlobDatum(Blob data) {
            this.data = data;
        }

        @Override
        public Object toJava(boolean success) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data.data));
                return ois.readObject();
            } catch (Exception e) {
                // TODO
                throw new RuntimeException(e);
            }

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

        public ErrorDatum(ErrorType type, String message) {
            this.type = Objects.requireNonNull(type);
            this.message = Objects.requireNonNull(message);
        }

        @Override
        public Object toJava(boolean success) {
            return null;
        }
    }

    public static class HttpReqDatum extends Datum {
        private final String method;
        private final Headers headers;
        private final byte[] body;

        private HttpReqDatum(String method, Headers headers, byte[] body) {
            this.method = Objects.requireNonNull(method);
            this.headers = Objects.requireNonNull(headers);
            this.body = body;
        }

        @Override
        public Object toJava(boolean success) {
            return null;
        }
    }

    public static class HttpRespDatum extends Datum {
        private final int status;
        private final Headers headers;
        private final byte[] body;

        public HttpRespDatum(int status, Headers headers, byte[] body) {
            this.status = status;
            this.headers = Objects.requireNonNull(headers);
            this.body = body;
        }

        @Override
        public Object toJava(boolean success) {
            return new FnResult() {
                @Override
                public byte[] getBodyAsBytes() {
                    return new byte[0];
                }

                @Override
                public String getBodyAsString() {
                    return null;
                }

                @Override
                public Headers getHeaders() {
                    return null;
                }

                @Override
                public int getStatus() {
                    return 0;
                }
            };
        }
    }

    public static class StageRefDatum extends Datum {
        private final String stageId;

        public StageRefDatum(String stageId) {
            this.stageId = stageId;
        }

        public String getStageId() {
            return stageId;
        }

        @Override
        public Object toJava(boolean success) {
            return new CompletionId(stageId);
        }
    }

}

