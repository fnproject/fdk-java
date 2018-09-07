package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

/**
 * Created on 22/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class APIModel {

    enum CompletionOperation {
        @JsonProperty("unknown_operation")
        UNKNOWN_OPERATION,
        @JsonProperty("acceptEither")
        ACCEPT_EITHER,
        @JsonProperty("applyToEither")
        APPLY_TO_EITHER,
        @JsonProperty("thenAcceptBoth")
        THEN_ACCEPT_BOTH,
        @JsonProperty("thenApply")
        THEN_APPLY,
        @JsonProperty("thenRun")
        THEN_RUN,
        @JsonProperty("thenAccept")
        THEN_ACCEPT,
        @JsonProperty("thenCompose")
        THEN_COMPOSE,
        @JsonProperty("thenCombine")
        THEN_COMBINE,
        @JsonProperty("whenComplete")
        WHEN_COMPLETE,
        @JsonProperty("handle")
        HANDLE(),
        @JsonProperty("supply")
        SUPPLY(),
        @JsonProperty("invokeFunction")
        INVOKE_FUNCTION(),
        @JsonProperty("completedValue")
        COMPLETED_VALUE(),
        @JsonProperty("delay")
        DELAY(),
        @JsonProperty("allOf")
        ALL_OF(),
        @JsonProperty("anyOf")
        ANY_OF(),
        @JsonProperty("externalCompletion")
        EXTERNAL_COMPLETION(),
        @JsonProperty("exceptionally")
        EXCEPTIONALLY(),
        @JsonProperty("terminationHook")
        TERMINATION_HOOK(),
        @JsonProperty("exceptionallyCompose")
        EXCEPTIONALLY_COMPOSE();


        CompletionOperation() {
        }


    }

    public static final class Blob {
        @JsonProperty("blob_id")
        public String blobId;

        @JsonProperty("length")
        public Long blobLength;

        @JsonProperty("content_type")
        public String contentType;

        public static Blob fromBlobResponse(BlobResponse blobResponse) {
            Blob blob= new Blob();
            blob.blobId = blobResponse.blobId;
            blob.contentType = blobResponse.contentType;
            blob.blobLength = blobResponse.blobLength;
            return blob;
        }
    }

    public static class CompletionResult {
        @JsonProperty("datum")
        public Datum result;

        @JsonProperty("successful")
        public boolean successful;


        public Object toJava(FlowId flowId, BlobStoreClient blobClient, ClassLoader classLoader) {
            return result.toJava(successful, flowId, blobClient, classLoader);
        }

        public static CompletionResult failure(Datum datum) {
            CompletionResult result = new CompletionResult();
            result.successful = false;
            result.result = datum;
            return result;
        }

        public static CompletionResult success(Datum datum) {
            CompletionResult result = new CompletionResult();
            result.successful = true;
            result.result = datum;
            return result;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
       @JsonSubTypes.Type(name = "empty", value = EmptyDatum.class),
       @JsonSubTypes.Type(name = "blob", value = BlobDatum.class),
       @JsonSubTypes.Type(name = "stage_ref", value = StageRefDatum.class),
       @JsonSubTypes.Type(name = "error", value = ErrorDatum.class),
       @JsonSubTypes.Type(name = "http_req", value = HTTPReqDatum.class),
       @JsonSubTypes.Type(name = "http_resp", value = HTTPRespDatum.class),
       @JsonSubTypes.Type(name = "status", value = StatusDatum.class),
    })

    public static abstract class Datum {
        public abstract Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader);

    }


    public static final class EmptyDatum extends Datum {

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            return null;
        }
    }

    public static final class BlobDatum extends Datum {
        @JsonUnwrapped
        public Blob blob;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            return blobStore.readBlob(flowId.getId(), blob.blobId, (requestInputStream) -> {

                try (ObjectInputStream ois = new ObjectInputStream(requestInputStream) {
                    @Override
                    protected Class resolveClass(ObjectStreamClass classDesc)
                       throws IOException, ClassNotFoundException {

                        String cname = classDesc.getName();
                        try {
                            return classLoader.loadClass(cname);
                        } catch (ClassNotFoundException ex) {
                            return super.resolveClass(classDesc);
                        }
                    }
                }) {

                    return ois.readObject();

                } catch (ClassNotFoundException | InvalidClassException | StreamCorruptedException | OptionalDataException e) {
                    throw new FunctionInputHandlingException("Error reading continuation content", e);
                } catch (IOException e) {
                    throw new PlatformException("Error reading blob data", e);
                }

            }, blob.contentType);
        }

        public static BlobDatum fromBlob(Blob blob) {
            BlobDatum datum = new BlobDatum();
            datum.blob = blob;
            return datum;
        }
    }

    public static final class StageRefDatum extends Datum {
        @JsonProperty("stage_id")
        public String stageId;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            return ((RemoteFlow) Flows.currentFlow()).createFlowFuture(new CompletionId(stageId));
        }
    }


    public enum ErrorType {
        @JsonProperty("unknown_error")
        UnknownError(),

        @JsonProperty("stage_timeout")
        StageTimeout(),

        @JsonProperty("stage_failed")
        StageFailed(),

        @JsonProperty("function_timeout")
        FunctionTimeout(),

        @JsonProperty("function_invoke_failed")
        FunctionInvokeFailed(),

        @JsonProperty("stage_lost")
        StageLost(),

        @JsonProperty("invalid_stage_response")
        InvalidStageResponse();


    }


    public static final class ErrorDatum extends Datum {
        @JsonProperty("type")
        public ErrorType type;
        @JsonProperty("message")
        public String message;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            switch (type) {
                case StageTimeout:
                    return new StageTimeoutException(message);
                case StageLost:
                    return new StageLostException(message);
                case StageFailed:
                    return new StageInvokeFailedException(message);
                case FunctionTimeout:
                    return new FunctionTimeoutException(message);
                case FunctionInvokeFailed:
                    return new FunctionInvokeFailedException(message);
                case InvalidStageResponse:
                    return new InvalidStageResponseException(message);
                default:
                    return new PlatformException(message);
            }
        }

        public static ErrorDatum newError(ErrorType type, String message) {
            ErrorDatum datum = new ErrorDatum();
            datum.type = type;
            datum.message = message;
            return datum;
        }
    }


    public enum HTTPMethod {
        @JsonProperty("unknown_method")
        UnknownMethod(HttpMethod.GET),
        @JsonProperty("get")
        Get(HttpMethod.GET),
        @JsonProperty("head")
        Head(HttpMethod.HEAD),
        @JsonProperty("post")
        Post(HttpMethod.POST),
        @JsonProperty("put")
        Put(HttpMethod.PUT),
        @JsonProperty("delete")
        Delete(HttpMethod.DELETE),
        @JsonProperty("options")
        Options(HttpMethod.OPTIONS),
        @JsonProperty("patch")
        Patch(HttpMethod.PATCH);

        final HttpMethod flowMethod;

        HTTPMethod(HttpMethod flowMethod) {
            this.flowMethod = flowMethod;
        }

        public static HTTPMethod fromFlow(HttpMethod f) {
            return Arrays.stream(values())
               .filter((x) -> x.flowMethod == f)
               .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid flow method"));
        }
    }

    public static final class HTTPHeader {
        @JsonProperty("key")
        public String key;

        @JsonProperty("value")
        public String value;


        public static HTTPHeader create(String key, String value) {
            HTTPHeader header = new HTTPHeader();
            header.key = key;
            header.value = value;
            return header;
        }
    }


    public static final class HTTPReq {
        @JsonProperty("body")
        public Blob body;

        @JsonProperty("headers")
        public List<HTTPHeader> headers;

        @JsonProperty("method")
        public HTTPMethod method;
    }

    public static final class HTTPReqDatum extends Datum {
        @JsonUnwrapped
        public HTTPReq req;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            return null;
        }
    }


    public enum StatusDatumType {
        @JsonProperty("unknown_state")
        UnknownState(Flow.FlowState.UNKNOWN),
        @JsonProperty("succeeded")
        Succeeded(Flow.FlowState.SUCCEEDED),
        @JsonProperty("failed")
        Failed(Flow.FlowState.FAILED),
        @JsonProperty("cancelled")
        Cancelled(Flow.FlowState.CANCELLED),
        @JsonProperty("killed")
        Killed(Flow.FlowState.KILLED);

        private final Flow.FlowState flowState;

        StatusDatumType(Flow.FlowState flowState) {
            this.flowState = flowState;
        }

        public Flow.FlowState getFlowState() {
            return flowState;
        }
    }

    public static final class StatusDatum extends Datum {

        @JsonProperty("type")
        public StatusDatumType type;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {
            return type.getFlowState();
        }

        public static StatusDatum fromType(StatusDatumType type) {
            StatusDatum datum = new StatusDatum();
            datum.type = type;
            return datum;
        }
    }

    public static final class HTTPResp {
        @JsonProperty("body")
        public Blob body;

        @JsonProperty("headers")
        public List<HTTPHeader> headers = new ArrayList<>();

        @JsonProperty("status_code")
        public Integer statusCode;
    }

    public static final class HTTPRespDatum extends Datum {
        @JsonUnwrapped
        public HTTPResp resp;

        @Override
        public Object toJava(boolean successful, FlowId flowId, BlobStoreClient blobStore, ClassLoader classLoader) {

            HttpResponse resp = new RemoteHTTPResponse(flowId, this.resp, blobStore);

            if (successful) {
                return resp;
            } else {
                return new FunctionInvocationException(resp);
            }
        }

        public static HTTPRespDatum create(HTTPResp res) {
            HTTPRespDatum datum = new HTTPRespDatum();
            datum.resp = res;
            return datum;
        }
    }

    public static Datum datumFromJava(FlowId flow, Object value, BlobStoreClient blobStore) {
        if (value == null) {
            return new EmptyDatum();
        } else if (value instanceof FlowFuture) {
            if (!(value instanceof RemoteFlow.RemoteFlowFuture)) {
                throw new IllegalStateException("Unsupported flow future type return by function");
            }
            StageRefDatum datum = new StageRefDatum();
            datum.stageId = ((RemoteFlow.RemoteFlowFuture) value).id();
            return datum;
        } else if (value instanceof RemoteHTTPResponse) {
            HTTPRespDatum datum = new HTTPRespDatum();
            datum.resp = ((RemoteHTTPResponse) value).response;
            return datum;
        } else if (value instanceof RemoteHTTPRequest) {
            HTTPReqDatum datum = new HTTPReqDatum();
            datum.req = ((RemoteHTTPRequest) value).req;
            return datum;
        } else {

            byte[] data;

            try {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(value);
                    oos.close();
                    data = bos.toByteArray();
                } catch (NotSerializableException e) {
                    if (value instanceof Throwable) {
                        // unserializable  errors are wrapped
                        WrappedFunctionException wrapped = new WrappedFunctionException((Throwable) value);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(wrapped);
                        oos.close();
                        data = bos.toByteArray();
                    } else {
                        throw new ResultSerializationException("Value not serializable", e);
                    }
                }


                BlobResponse blobResponse = blobStore.writeBlob(flow.getId(), data, RemoteFlowApiClient.CONTENT_TYPE_JAVA_OBJECT);
                APIModel.Blob blob = APIModel.Blob.fromBlobResponse(blobResponse);

                BlobDatum bd = new BlobDatum();
                bd.blob = blob;
                return bd;
            } catch (IOException e) {
                throw new PlatformCommunicationException("Failed to store blob", e);
            }

        }
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
        public CreateGraphRequest() {
        }

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
        @JsonProperty("flow_id")
        public String flowId;

        @JsonProperty("stage_id")
        public String stageId;

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
        public List<CompletionResult> args = new ArrayList<>();
    }

    public static class InvokeStageResponse {
        @JsonProperty("result")
        public CompletionResult result;

    }

    private static class RemoteHTTPResponse implements HttpResponse {

        private final HTTPResp response;
        private final BlobStoreClient blobStoreClient;
        private final FlowId flowId;

        byte[] body;

        RemoteHTTPResponse(FlowId flow, HTTPResp response, BlobStoreClient blobStoreClient) {
            this.response = response;
            this.blobStoreClient = blobStoreClient;
            this.flowId = flow;
        }

        @Override
        public int getStatusCode() {
            return response.statusCode;
        }

        @Override
        public Headers getHeaders() {
            Map<String, String> headers = new HashMap<>();
            response.headers.forEach((h) -> headers.put(h.key, h.value));
            return Headers.fromMap(headers);
        }

        @Override
        public byte[] getBodyAsBytes() {
            if (body != null) {
                return body;
            }
            return body = readBlobDataAsBytes(blobStoreClient, flowId, response.body);

        }

    }


    private static class RemoteHTTPRequest implements HttpRequest {

        private final HTTPReq req;
        private final BlobStoreClient blobStoreClient;
        private final FlowId flowId;

        byte[] body;

        RemoteHTTPRequest(FlowId flow, HTTPResp response, HTTPReq req, BlobStoreClient blobStoreClient) {
            this.req = req;
            this.blobStoreClient = blobStoreClient;
            this.flowId = flow;
        }


        @Override
        public HttpMethod getMethod() {
            return req.method.flowMethod;
        }

        @Override
        public Headers getHeaders() {
            Map<String, String> headers = new HashMap<>();
            req.headers.forEach((h) -> headers.put(h.key, h.value));
            return Headers.fromMap(headers);
        }

        @Override
        public byte[] getBodyAsBytes() {
            if (body != null) {
                return body;
            }
            return body = readBlobDataAsBytes(blobStoreClient, flowId, req.body);


        }

    }

    private static byte[] readBlobDataAsBytes(BlobStoreClient blobStoreClient, FlowId flowId, Blob blob) {

        if (blob != null) {
            return blobStoreClient.readBlob(flowId.getId(), blob.blobId, (inputStream) -> {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(inputStream, byteArrayOutputStream);
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    throw new FunctionInputHandlingException("Unable to read blob");
                }
            }, blob.contentType);

        } else {
            return new byte[0];
        }
    }
}
