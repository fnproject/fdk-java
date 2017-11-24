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

    public static class CompletionResult {
        @JsonProperty("datum")
        public Datum result;

        @JsonProperty("successful")
        public Boolean successful;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "empty", value = EmptyDatum.class),
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
        public Blob blob;
    }

    public static final class StageRefDatum extends Datum {
        @JsonProperty("stage_id")
        public String stageId;
    }


    public enum ErrorType {
        UnknownError("unknown_error"),
        StageTimeout("stage_timeout"),
        StageFailed("stage_failed"),
        FunctionTimeout("function_timeout"),
        FunctionInvokeFailed("function_invoke_failed"),
        StageLost("stage_lost"),
        InvalidStageResponse("invalid_stage_response");

        private String errorType;

        ErrorType(String errorType) {
            this.errorType = errorType;
        }

        @JsonValue
        String getName() {
            return errorType;
        }
    }


    public static final class ErrorDatum extends Datum {
        @JsonProperty("type")
        public ErrorType type;
        @JsonProperty("message")
        public String message;
    }


    public enum HTTPMethod {
        UnknownMethod("unknown_method"),
        Get("get"),
        Head("head"),
        Post("post"),
        Put("put"),
        Delete("delete"),
        Options("options"),
        Patch("patch");

        private String method;

        HTTPMethod(String method) {
            this.method = method;
        }

        @JsonValue
        String getName() {
            return method;
        }

        public static HTTPMethod fromString(String method) {
            for (HTTPMethod b : HTTPMethod.values()) {
                if (b.method.equalsIgnoreCase(method)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static final class HTTPHeader {
        @JsonProperty("key")
        public String key;

        @JsonProperty("value")
        public String value;
    }


    public static final class HTTPReq {
        @JsonProperty("arg")
        public Blob body;

        @JsonProperty("headers")
        public List<HTTPHeader> headers;

        @JsonProperty("method")
        public HTTPMethod method;
    }

    public static final class HTTPReqDatum extends Datum {
        @JsonUnwrapped
        public HTTPReq req;

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
        public StateDatumType type;
    }

    public static final class HTTPResp {
        @JsonProperty("body")
        public Blob body;

        @JsonProperty("headers")
        public List<HTTPHeader> headers;

        @JsonProperty("status_code")
        public Integer statusCode;
    }

    public static final class HTTPRespDatum extends Datum {
        @JsonUnwrapped
        public HTTPResp resp;

    }

    public static class AddStageResponse {
        @JsonProperty("flow_id")
        public String flowId;

        @JsonProperty("stage_id")
        public String stageId;
    }

    static class CreateGraphResponse {
        @JsonProperty("flow_id")
        public String flowId;
    }

    public static class CreateGraphRequest {
        public CreateGraphRequest(String functionId) {
            this.functionId = functionId;
        }

        @JsonProperty("function_id")
        public String functionId;
    }


    public static class AddStageRequest {
        @JsonProperty("operation")
        public CompletionOperation operation;

        @JsonProperty("closure")
        public Blob closure;

        @JsonProperty("deps")
        public List<String> deps;

        @JsonProperty("code_location")
        public String codeLocation;

        @JsonProperty("caller_id")
        public String callerId;
    }


    public static class CompleteStageExternallyRequest {
        @JsonProperty("value")
        public CompletionResult value;

        @JsonProperty("code_location")
        public String codeLocation;

        @JsonProperty("caller_id")
        public String callerId;
    }


    public static class AddCompletedValueStageRequest {
        @JsonProperty("value")
        public CompletionResult value;

        @JsonProperty("code_location")
        public String codeLocation;

        @JsonProperty("caller_id")
        public String callerId;
    }

    public static class AddDelayStageRequest {
        @JsonProperty("delay_ms")
        public Long delayMs;

        @JsonProperty("code_location")
        public String codeLocation;

        @JsonProperty("caller_id")
        public String callerId;
    }


    public static class AddInvokeFunctionStageRequest {
        @JsonProperty("function_id")
        public String functionId;

        @JsonProperty("arg")
        public HTTPReq arg;

        @JsonProperty("code_location")
        public String codeLocation;

        @JsonProperty("caller_id")
        public String callerId;
    }


    public static class AwaitStageResponse {
        @JsonProperty("result")
        public CompletionResult result;
    }

    public static class InvokeStageRequest {
        @JsonProperty("flow_id")
        public String flowId;

        @JsonProperty("stage_id")
        public String stageId;

        @JsonProperty("closure")
        public Blob closure;

        @JsonProperty("args")
        public List<CompletionResult> args;
    }

    public static class InvokeStageResponse {
        @JsonProperty("result")
        public CompletionResult result;

    }
}
