package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

/**
 * Created on 22/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class APIModel {


    enum CompletionOperation {
        UNKNOWN_OPERATION("unknown_operation"),
        ACCEPT_EITHER("acceptEither"),
        APPLY_TO_EITHER("applyToEither"),
        THEN_ACCEPT_BOTH("thenAcceptBoth"),
        THEN_APPLY("thenApply"),
        THEN_RUN("thenRun"),
        THEN_ACCEPT("thenAccept"),
        THEN_COMPOSE("thenCompose"),
        THEN_COMBINE("thenCombine"),
        WHEN_COMPLETE("whenComplete"),
        HANDLE("handle"),
        SUPPLY("supply"),
        INVOKE_FUNCTION("invokeFunction"),
        COMPLETED_VALUE("completedValue"),
        DELAY("delay"),
        ALL_OF("allOf"),
        ANY_OF("anyOf"),
        EXTERNAL_COMPLETION("externalCompletion"),
        EXCEPTIONALLY("exceptionally"),
        TERMINATION_HOOK("terminationHook"),
        EXCEPTIONALLY_COMPOSE("exceptionallyCompose");

        private String operation;

        CompletionOperation(String operation) {
            this.operation = operation;
        }

        @JsonValue
        String getName() {
            return operation;
        }
    }

    public static final class Blob {
        @JsonProperty("blob_id")
        public String blobId;

        @JsonProperty("length")
        public Long blobLength;

        @JsonProperty("content_type")
        public String contentType;
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({@JsonSubTypes.Type(name = "empty", value = EmptyDatum.class),
       @JsonSubTypes.Type(name = "blob", value = BlobDatum.class),
       @JsonSubTypes.Type(name = "stage_ref", value = StageRefDatum.class),
       @JsonSubTypes.Type(name = "error", value = ErrorDatum.class),
       @JsonSubTypes.Type(name = "http_req", value = HTTPReqDatum.class),
       @JsonSubTypes.Type(name = "http_resp", value = HTTPRespDatum.class),
       @JsonSubTypes.Type(name = "state", value = StateDatum.class),

    })


    public static abstract class Datum {

    }


    public static final class EmptyDatum extends Datum {

    }

    public static final class BlobDatum extends Datum {
        @JsonUnwrapped
        Blob blob;
    }

    public static final class StageRefDatum extends Datum {
        @JsonProperty("stage_id")
        String stageId;
    }


    public enum ErrorType {
        UnknownError,
        StageTimeout,
        StageFailed,
        FunctionTimeout,
        FunctionInvokeFailed,
        StageLost,
        InvalidStageResponse
    }


    public static final class ErrorDatum extends Datum {
        @JsonProperty("type")
        ErrorType type;
        @JsonProperty("message")
        String message;
    }


    public enum HTTPMethod {
        UnknownMethod,
        Get,
        Head,
        Post,
        Put,
        Delete,
        Options,
        Patch
    }

    public static final class HTTPHeader {
        String key;
        String value;
    }

    public static final class HTTPReqDatum extends Datum {
        @JsonProperty("body")
        Blob body;

        @JsonProperty("headers")
        List<HTTPHeader> headers;

        @JsonProperty("method")
        HTTPMethod method;

    }


    public enum StateDatumType {
        UnknownState,
        Succeeded,
        Failed,
        Cancelled,
        Killed
    }

    public static final class StateDatum extends Datum {

        @JsonProperty("type")
        StateDatumType type;
    }

    public static final class HTTPRespDatum extends Datum {
        @JsonProperty("body")
        Blob body;

        @JsonProperty("status_code")
        Integer statusCode;

    }

    public static class AddStageResponse {
        @JsonProperty("flow_id")
        String flowId;
        @JsonProperty("stage_id")
        String stageId;
    }

    static class CreateGraphResponse {
        @JsonProperty("flow_id")
        String flowId;
    }

    public static class CreateGraphRequest {
        public CreateGraphRequest(String functionId) {
            this.functionId = functionId;
        }

        @JsonProperty("function_id")
        String functionId;
    }



    public static class AddStageRequest {
        @JsonProperty("operation")
        CompletionOperation operation;
        @JsonProperty("closure")
        Blob closure;
        @JsonProperty("deps")
        List<String> deps;
        @JsonProperty("code_location")
        String codeLocation;
        @JsonProperty("caller_id")
        String callerId;
    }
}
