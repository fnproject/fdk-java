package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.HttpMethod;
import com.fnproject.fn.api.flow.LambdaSerializationException;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.api.flow.ResultSerializationException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST client for accessing the Flow service API
 */
public class RemoteFlowApiClient implements CompleterClient {
    private transient final HttpClient httpClient;
    private final String apiUrlBase;

    private static final String HEADER_PREFIX = "FnProject-";
    public static final String STAGE_ID_HEADER = HEADER_PREFIX + "StageID";
    public static final String CALLER_ID_HEADER = HEADER_PREFIX + "CallerID";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_TYPE_JAVA_OBJECT = "application/java-serialized-object";

    public static final String FN_CODE_LOCATION = HEADER_PREFIX + "Codeloc";

    public static final String DATUM_TYPE_HEADER = HEADER_PREFIX + "DatumType";
    public static final String DATUM_TYPE_BLOB = "blob";
    public static final String DATUM_TYPE_ERROR = "error";
    public static final String DATUM_TYPE_STAGEREF = "stageref";
    public static final String DATUM_TYPE_HTTP_REQ = "httpreq";
    public static final String DATUM_TYPE_HTTP_RESP = "httpresp";

    public static final String RESULT_STATUS_HEADER = HEADER_PREFIX + "ResultStatus";
    public static final String RESULT_STATUS_SUCCESS = "success";
    public static final String RESULT_STATUS_FAILURE = "failure";


    public RemoteFlowApiClient(String apiUrlBase, HttpClient httpClient) {
        this.apiUrlBase = Objects.requireNonNull(apiUrlBase);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    class CreateGraphRequest {
        public CreateGraphRequest(String functionId) {
            this.functionId = functionId;
        }

        @JsonProperty("function_id")
        String functionId;
    }

    @Override
    public FlowId createFlow(String functionId) {
        try {
            CreateGraphRequest createGraphRequest = new CreateGraphRequest(functionId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(baos, createGraphRequest);
            HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/v1/flow/create").withBody(baos.toByteArray());
            try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
                validateSuccessful(resp);
                CreateGraphResponse createGraphResponse = objectMapper.readValue(resp.body, CreateGraphResponse.class);
                return new FlowId(createGraphResponse.graphId);
            } catch (Exception e) {
                throw new PlatformCommunicationException("Failed to create flow ", e);
            }
        } catch (IOException e) {
            throw new PlatformException("Failed to create CreateGraphRequest");
        }
    }

    @Override
    public CompletionId supply(FlowId flowId, Serializable supplier, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenApply(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId whenComplete(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return null;
    }


    public CompletionId createCompletion(FlowId flowId, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId completedValue(FlowId flowId, boolean success, Object value, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId allOf(FlowId flowId, List<CompletionId> deps, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId handle(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId exceptionallyCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable fn, CompletionId alternate, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public boolean complete(FlowId flowId, CompletionId completionId, Object value) {
        return false;
    }

    @Override
    public boolean completeExceptionally(FlowId flowId, CompletionId completionId, Throwable value) {
        return false;
    }

    @Override
    public CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return null;
    }

    @Override
    public CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation) {
        return null;
    }

    // wait for completion  -> result
    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored, long timeout, TimeUnit unit) throws TimeoutException {
        return null;
    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored) {
        return null;
    }

    public void commit(FlowId flowId) {
        try (HttpClient.HttpResponse response = httpClient.execute(HttpClient.preparePost(apiUrlBase + "/graph/" + flowId.getId() + "/commit"))) {
            validateSuccessful(response);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    @Override
    public void addTerminationHook(FlowId flowId, Serializable code, CodeLocation codeLocation) {
    }

    private static void validateSuccessful(HttpClient.HttpResponse response) {
        if (!isSuccessful(response)) {
            try {
                String body = response.entityAsString();
                throw new PlatformException(String.format("Received unexpected response (%d) from " +
                        "completer: %s", response.getStatusCode(), body == null ? "Empty body" : body));
            } catch (IOException e) {
                throw new PlatformException(String.format("Received unexpected response (%d) from " +
                        "completer. Could not read body.", response.getStatusCode()), e);
            }
        }
    }

    private static boolean isSuccessful(HttpClient.HttpResponse response) {
        return response.getStatusCode() == 200 || response.getStatusCode() == 201;
    }

    private CompletionId requestCompletion(String url, Function<HttpClient.HttpRequest, HttpClient.HttpRequest> fn) {
        HttpClient.HttpRequest req = fn.andThen(RemoteFlowApiClient::addParentIdIfPresent)
                .apply(HttpClient.preparePost(apiUrlBase + url));

        try (HttpClient.HttpResponse resp = httpClient.execute(req)) {
            validateSuccessful(resp);
            String completionId = resp.getHeader(STAGE_ID_HEADER);
            if (completionId == null) {
                throw new PlatformException("Got successful response from completer but no " + STAGE_ID_HEADER + " was present");
            }
            return new CompletionId(completionId);
        } catch (PlatformException e) {
            throw e;
        } catch (Exception e) {
            throw new PlatformException("Failed to get response from completer: ", e);
        }
    }


    private static  HttpClient.HttpRequest addParentIdIfPresent(HttpClient.HttpRequest req) {
        return FlowRuntimeGlobals.getCurrentCompletionId().map((id) -> req.withHeader(CALLER_ID_HEADER, id.getId())).orElse(req);
    }
}
