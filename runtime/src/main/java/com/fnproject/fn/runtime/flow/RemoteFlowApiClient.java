package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.FlowCompletionException;
import com.fnproject.fn.api.flow.HttpMethod;
import com.fnproject.fn.api.flow.LambdaSerializationException;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.fnproject.fn.runtime.flow.HttpClient.prepareGet;
import static com.fnproject.fn.runtime.flow.HttpClient.preparePost;


/**
 * REST client for accessing the Flow service API
 */
public class RemoteFlowApiClient implements CompleterClient {
    public static final String CONTENT_TYPE_HEADER = "Content-type";
    private transient final HttpClient httpClient;
    private final String apiUrlBase;
    private final BlobStoreClient blobStoreClient;
    public static final String CONTENT_TYPE_JAVA_OBJECT = "application/java-serialized-object";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";


    public RemoteFlowApiClient(String apiUrlBase, BlobStoreClient blobClient, HttpClient httpClient) {
        this.apiUrlBase = Objects.requireNonNull(apiUrlBase);
        this.blobStoreClient = Objects.requireNonNull(blobClient);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public FlowId createFlow(String functionId) {
        try {
            APIModel.CreateGraphRequest createGraphRequest = new APIModel.CreateGraphRequest(functionId);
            ObjectMapper objectMapper = FlowRuntimeGlobals.getObjectMapper();
            byte[] body = objectMapper.writeValueAsBytes(createGraphRequest);
            HttpClient.HttpRequest request = preparePost(apiUrlBase + "/flows").withBody(body);
            try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
                validateSuccessful(resp);
                APIModel.CreateGraphResponse createGraphResponse = objectMapper.readValue(resp.body, APIModel.CreateGraphResponse.class);
                return new FlowId(createGraphResponse.flowId);
            } catch (Exception e) {
                throw new PlatformCommunicationException("Failed to create flow ", e);
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to create CreateGraphRequest");
        }
    }

    @Override
    public CompletionId supply(FlowId flowId, Serializable supplier, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.SUPPLY, flowId, supplier, codeLocation, Collections.emptyList());
    }

    @Override
    public CompletionId thenApply(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_APPLY, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_COMPOSE, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId whenComplete(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.WHEN_COMPLETE, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_ACCEPT, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_RUN, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.ACCEPT_EITHER, flowId, fn, codeLocation, Arrays.asList(completionId, alternate));

    }

    @Override
    public CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.APPLY_TO_EITHER, flowId, fn, codeLocation, Arrays.asList(completionId, alternate));

    }

    @Override
    public CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_ACCEPT_BOTH, flowId, fn, codeLocation, Arrays.asList(completionId, alternate));
    }

    @Override
    public CompletionId createCompletion(FlowId flowId, CodeLocation codeLocation) {
        return addStage(APIModel.CompletionOperation.EXTERNAL_COMPLETION, null, Collections.emptyList(), flowId, codeLocation);
    }

    @Override
    public CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation) {

        APIModel.HTTPReq httpReq = new APIModel.HTTPReq();

        if (headers != null) {
            if (data.length > 0) {
                BlobResponse blobResponse = blobStoreClient.writeBlob(flowId.getId(), data, headers.get(CONTENT_TYPE_HEADER).orElse(CONTENT_TYPE_OCTET_STREAM));
                httpReq.body = APIModel.Blob.fromBlobResponse(blobResponse);
            }

            httpReq.headers = new ArrayList<>();

            headers.getAll().forEach((k, v) -> {
                APIModel.HTTPHeader h = new APIModel.HTTPHeader();
                h.key = k;
                h.value = v;
                httpReq.headers.add(h);
            });
        }

        httpReq.method = APIModel.HTTPMethod.fromFlow(method);

        APIModel.AddInvokeFunctionStageRequest addInvokeFunctionStageRequest = new APIModel.AddInvokeFunctionStageRequest();
        addInvokeFunctionStageRequest.arg = httpReq;
        addInvokeFunctionStageRequest.codeLocation = codeLocation.getLocation();
        addInvokeFunctionStageRequest.callerId = FlowRuntimeGlobals.getCurrentCompletionId().map(CompletionId::getId).orElse(null);
        addInvokeFunctionStageRequest.functionId = functionId;

        try {
            return executeAddInvokeFunctionStageRequest(flowId, addInvokeFunctionStageRequest);
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to create invokeFunction stage", e);
        }
    }

    @Override
    public CompletionId completedValue(FlowId flowId, boolean success, Object value, CodeLocation codeLocation) {
        try {
            APIModel.AddCompletedValueStageRequest addCompletedValueStageRequest = new APIModel.AddCompletedValueStageRequest();
            addCompletedValueStageRequest.callerId = FlowRuntimeGlobals.getCurrentCompletionId().map(CompletionId::getId).orElse(null);

            addCompletedValueStageRequest.codeLocation = codeLocation.getLocation();
            APIModel.CompletionResult completionResult = new APIModel.CompletionResult();
            completionResult.successful = success;

            if(value instanceof RemoteFlow.RemoteFlowFuture) {
                APIModel.StageRefDatum stageRefDatum = new APIModel.StageRefDatum();
                stageRefDatum.stageId = ((RemoteFlow.RemoteFlowFuture)value).id();
                completionResult.result = stageRefDatum;
            } else {
                APIModel.Datum blobDatum = APIModel.datumFromJava(flowId, value, blobStoreClient);
                completionResult.result = blobDatum;
            }
            addCompletedValueStageRequest.value = completionResult;

            return executeAddCompletedValueStageRequest(flowId, addCompletedValueStageRequest);
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to create completedValue stage", e);
        }
    }

    @Override
    public CompletionId allOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return addStage(APIModel.CompletionOperation.ALL_OF, null, cids, flowId, codeLocation);
    }

    @Override
    public CompletionId handle(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.HANDLE, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.EXCEPTIONALLY, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId exceptionallyCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.EXCEPTIONALLY_COMPOSE, flowId, fn, codeLocation, Collections.singletonList(completionId));
    }

    @Override
    public CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable fn, CompletionId alternate, CodeLocation codeLocation) {
        return addStageWithClosure(APIModel.CompletionOperation.THEN_COMBINE, flowId, fn, codeLocation, Arrays.asList(completionId, alternate));
    }

    @Override
    public boolean complete(FlowId flowId, CompletionId completionId, Object value, CodeLocation codeLocation) {
        try {
            APIModel.Datum blobDatum = APIModel.datumFromJava(flowId, value, blobStoreClient);
            APIModel.CompletionResult completionResult = new APIModel.CompletionResult();
            completionResult.result = blobDatum;
            completionResult.successful = true;

            return completeStageExternally(flowId, completionId, completionResult, codeLocation);
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to complete stage externally", e);
        }
    }

    @Override
    public boolean completeExceptionally(FlowId flowId, CompletionId completionId, Throwable value, CodeLocation codeLocation) {
        try {
            APIModel.Datum blobDatum = APIModel.datumFromJava(flowId, value, blobStoreClient);
            APIModel.CompletionResult completionResult = new APIModel.CompletionResult();
            completionResult.result = blobDatum;
            completionResult.successful = false;

            return completeStageExternally(flowId, completionId, completionResult, codeLocation);
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to complete stage externally", e);
        }
    }

    @Override
    public CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return addStage(APIModel.CompletionOperation.ANY_OF, null, cids, flowId, codeLocation);
    }

    @Override
    public CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation) {
        try {
            APIModel.AddDelayStageRequest addDelayStageRequest = new APIModel.AddDelayStageRequest();
            addDelayStageRequest.callerId = FlowRuntimeGlobals.getCurrentCompletionId().map(CompletionId::getId).orElse(null);
            addDelayStageRequest.codeLocation = codeLocation.getLocation();
            addDelayStageRequest.delayMs = l;
            return executeAddDelayStageRequest(flowId, addDelayStageRequest);
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to create completedValue stage", e);
        }
    }

    // wait for completion  -> result
    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored, long timeout, TimeUnit unit) throws TimeoutException {
        long msTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
        long start = System.currentTimeMillis();
        do {
            long lastStart = System.currentTimeMillis();

            long remainingTimeout = Math.max(1, start + msTimeout - lastStart);

            try (HttpClient.HttpResponse response =
                    httpClient.execute(prepareGet(apiUrlBase + "/flows/" + flowId.getId() + "/stages/" + id.getId() + "/await?timeout_ms=" + remainingTimeout))) {

                if (response.getStatusCode() == 200) {
                    APIModel.AwaitStageResponse resp = FlowRuntimeGlobals.getObjectMapper().readValue(response.getContentStream(), APIModel.AwaitStageResponse.class);
                    if (resp.result.successful) {
                        return resp.result.toJava(flowId, blobStoreClient, getClass().getClassLoader());
                    } else {
                        throw new FlowCompletionException((Throwable)resp.result.toJava(flowId, blobStoreClient, getClass().getClassLoader()));
                    }
                } else if (response.getStatusCode() == 408) {
                    // do nothing go round again
                } else {
                    throw asError(response);
                }

                try {
                    Thread.sleep(Math.max(0, 500 - (System.currentTimeMillis() - lastStart)));
                } catch (InterruptedException e) {
                    throw new PlatformCommunicationException("Interrupted", e);
                }
            } catch (IOException e) {
                throw new PlatformCommunicationException("Error fetching result", e);
            }


        } while (System.currentTimeMillis() - start < msTimeout);

        throw new TimeoutException("Stage did not completed before timeout ");
    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored) {
        try {
            return waitForCompletion(flowId, id, ignored, 10, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            throw new PlatformCommunicationException("timeout", e);
        }
    }

    public void commit(FlowId flowId) {
        try (HttpClient.HttpResponse response = httpClient.execute(preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/commit"))) {
            validateSuccessful(response);
        } catch (Exception e) {
            throw new PlatformCommunicationException("Failed to commit graph", e);
        }
    }

    @Override
    public void addTerminationHook(FlowId flowId, Serializable code, CodeLocation codeLocation) {
        addStageWithClosure(APIModel.CompletionOperation.TERMINATION_HOOK, flowId, code, codeLocation, Collections.emptyList());
    }

    private static void validateSuccessful(HttpClient.HttpResponse response) {
        if (!isSuccessful(response)) {
            throw asError(response);
        }
    }

    private static PlatformCommunicationException asError(HttpClient.HttpResponse response) {
        try {
            String body = response.entityAsString();
            return new PlatformCommunicationException(String.format("Received unexpected response (%d) from " +
               "Flow service: %s", response.getStatusCode(), body == null ? "Empty body" : body));
        } catch (IOException e) {
            return new PlatformCommunicationException(String.format("Received unexpected response (%d) from " +
               "Flow service. Could not read body.", response.getStatusCode()), e);
        }
    }

    private static boolean isSuccessful(HttpClient.HttpResponse response) {
        return response.getStatusCode() == 200 || response.getStatusCode() == 201;
    }

    private static byte[] serializeClosure(Object data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(data);
            oos.close();
            return bos.toByteArray();
        } catch (NotSerializableException nse) {
            throw new LambdaSerializationException("Closure not serializable", nse);
        } catch (IOException e) {
            throw new PlatformException("Failed to write closure", e);
        }

    }

    private CompletionId addStageWithClosure(APIModel.CompletionOperation operation, FlowId flowId, Serializable supplier, CodeLocation codeLocation, List<CompletionId> deps) {

        byte[] serialized = serializeClosure(supplier);
        BlobResponse blobResponse   = blobStoreClient.writeBlob(flowId.getId(), serialized, CONTENT_TYPE_JAVA_OBJECT);

        return addStage(operation, APIModel.Blob.fromBlobResponse(blobResponse), deps, flowId, codeLocation);

    }

    private CompletionId addStage(APIModel.CompletionOperation operation, APIModel.Blob closure, List<CompletionId> deps, FlowId flowId, CodeLocation codeLocation) {
        try {
            APIModel.AddStageRequest addStageRequest = new APIModel.AddStageRequest();
            addStageRequest.closure = closure;
            addStageRequest.operation = operation;
            addStageRequest.codeLocation = codeLocation.getLocation();
            addStageRequest.callerId = FlowRuntimeGlobals.getCurrentCompletionId().map(CompletionId::getId).orElse(null);
            addStageRequest.deps = deps.stream().map(dep -> dep.getId()).collect(Collectors.toList());

            return executeAddStageRequest(flowId, addStageRequest);
        } catch (IOException e) {
            throw new PlatformException("Failed to add stage", e);
        }
    }

    private CompletionId executeAddStageRequest(FlowId flowId, APIModel.AddStageRequest addStageRequest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlowRuntimeGlobals.getObjectMapper().writeValue(baos, addStageRequest);

        HttpClient.HttpRequest request = preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/stage").withBody(baos.toByteArray());
        return requestForCompletionId(request);
    }

    private CompletionId executeAddCompletedValueStageRequest(FlowId flowId, APIModel.AddCompletedValueStageRequest addCompletedValueStageRequest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlowRuntimeGlobals.getObjectMapper().writeValue(baos, addCompletedValueStageRequest);

        HttpClient.HttpRequest request = preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/value").withBody(baos.toByteArray());
        return requestForCompletionId(request);
    }

    private CompletionId requestForCompletionId(HttpClient.HttpRequest request) {
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            validateSuccessful(resp);
            APIModel.AddStageResponse addStageResponse = FlowRuntimeGlobals.getObjectMapper().readValue(resp.body, APIModel.AddStageResponse.class);
            return new CompletionId(addStageResponse.stageId);
        } catch (Exception e) {
            throw new PlatformCommunicationException("Failed to add stage ", e);
        }
    }

    private CompletionId executeAddInvokeFunctionStageRequest(FlowId flowId, APIModel.AddInvokeFunctionStageRequest addInvokeFunctionStageRequest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlowRuntimeGlobals.getObjectMapper().writeValue(baos, addInvokeFunctionStageRequest);

        byte[] bytes = baos.toByteArray();
        return requestForCompletionId(preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/invoke").withBody(bytes));
    }

    private CompletionId executeAddDelayStageRequest(FlowId flowId, APIModel.AddDelayStageRequest addDelayStageRequest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlowRuntimeGlobals.getObjectMapper().writeValue(baos, addDelayStageRequest);

        byte[] bytes = baos.toByteArray();
        HttpClient.HttpRequest request = preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/delay").withBody(bytes);
        return requestForCompletionId(request);
    }

    private boolean completeStageExternally(FlowId flowId, CompletionId completionId, APIModel.CompletionResult completionResult, CodeLocation codeLocation) throws IOException {
        APIModel.CompleteStageExternallyRequest completeStageExternallyRequest = new APIModel.CompleteStageExternallyRequest();
        completeStageExternallyRequest.callerId = FlowRuntimeGlobals.getCurrentCompletionId().map(CompletionId::getId).orElse(null);
        completeStageExternallyRequest.value = completionResult;
        completeStageExternallyRequest.codeLocation = codeLocation.getLocation();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FlowRuntimeGlobals.getObjectMapper().writeValue(baos, completeStageExternallyRequest);
        byte[] bytes = baos.toByteArray();
        HttpClient.HttpRequest request = preparePost(apiUrlBase + "/flows/" + flowId.getId() + "/stages/" + completionId.getId() + "/complete").withBody(bytes);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            return isSuccessful(resp);
        } catch (Exception e) {
            throw new PlatformCommunicationException("Failed to add stage ", e);
        }
    }
}
