package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;



/**
 * REST client for accessing completer API
 */
public class RemoteCompleterApiClient implements CompleterClient {
    private transient final HttpClient httpClient;
    private final String apiUrlBase;

    // TODO: move these to SerUtils

    private static final String HEADER_PREFIX = "FnProject-";
    public static final String FLOW_ID_HEADER = HEADER_PREFIX + "FlowID";
    public static final String STAGE_ID_HEADER = HEADER_PREFIX + "StageID";
    public static final String METHOD_HEADER = HEADER_PREFIX + "Method";
    public static final String USER_HEADER_PREFIX = HEADER_PREFIX + "Header-";
    public static final String STATE_TYPE_HEADER = HEADER_PREFIX + "Statetype";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_TYPE_JAVA_OBJECT = "application/java-serialized-object";
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public static final String FN_CODE_LOCATION = HEADER_PREFIX + "Codeloc";

    public static final String DATUM_TYPE_HEADER = HEADER_PREFIX + "DatumType";
    public static final String DATUM_TYPE_BLOB = "blob";
    public static final String DATUM_TYPE_EMPTY = "empty";
    public static final String DATUM_TYPE_ERROR = "error";
    public static final String DATUM_TYPE_STATE = "state";
    public static final String DATUM_TYPE_STAGEREF = "stageref";
    public static final String DATUM_TYPE_HTTP_REQ = "httpreq";
    public static final String DATUM_TYPE_HTTP_RESP = "httpresp";

    public static final String RESULT_STATUS_HEADER = HEADER_PREFIX + "ResultStatus";
    public static final String RESULT_STATUS_SUCCESS = "success";
    public static final String RESULT_STATUS_FAILURE = "failure";

    public static final String REQUEST_METHOD_HEADER = HEADER_PREFIX + "Method";

    public static final String RESULT_CODE_HEADER = HEADER_PREFIX + "ResultCode";

    public static final String ERROR_TYPE_HEADER = HEADER_PREFIX + "ErrorType";
    public static final String ERROR_TYPE_STAGE_TIMEOUT = "stage-timeout";
    public static final String ERROR_TYPE_STAGE_INVOKE_FAILED = "stage-invoke-failed";
    public static final String ERROR_TYPE_FUNCTION_TIMEOUT = "function-timeout";
    public static final String ERROR_TYPE_FUNCTION_INVOKE_FAILED = "function-invoke-failed";
    public static final String ERROR_TYPE_STAGE_LOST = "stage-lost";
    public static final String ERROR_TYPE_INVALID_STAGE_RESPONSE = "invalid-stage-response";

    private static final int MAX_POLL_INTERVAL_MS = 1000;
    private static final int HTTP_CODE_REQUEST_TIMEOUT = 408;

    public RemoteCompleterApiClient(String apiUrlBase, HttpClient httpClient) {
        this.apiUrlBase = Objects.requireNonNull(apiUrlBase);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public FlowId createFlow(String functionId) {
        HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/graph?functionId=" + functionId);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            validateSuccessful(resp);
            return new FlowId(resp.getHeader(FLOW_ID_HEADER));
        } catch (Exception e) {
            throw new PlatformCommunicationException("Failed to create flow: " + e.getMessage());
        }
    }

    @Override
    public CompletionId supply(FlowId flowId, Serializable supplier, CodeLocation codeLocation) {
        return requestCompletionWithBody("/graph/" + flowId.getId() + "/supply", Function.identity(), supplier, codeLocation);
    }

    @Override
    public CompletionId thenApply(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "thenApply", fn, codeLocation);
    }

    @Override
    public CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "thenCompose", fn, codeLocation);
    }

    @Override
    public CompletionId whenComplete(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "whenComplete", fn, codeLocation);
    }

    @Override
    public CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "thenAccept", fn, codeLocation);
    }

    @Override
    public CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "thenRun", fn, codeLocation);
    }

    @Override
    public CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return chainThisWithThat(flowId, completionId, alternate, "acceptEither", fn, codeLocation);
    }

    @Override
    public CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return chainThisWithThat(flowId, completionId, alternate, "applyToEither", fn, codeLocation);
    }

    @Override
    public CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation) {
        return chainThisWithThat(flowId, completionId, alternate, "thenAcceptBoth", fn, codeLocation);
    }

    @Override
    public ExternalCompletion createExternalCompletion(FlowId flowId) {
        CompletionId completionId = requestCompletion("/graph/" + flowId.getId() + "/externalCompletion", Function.identity());
        return new ExternalCompletion() {
            @Override
            public CompletionId completionId() {
                return completionId;
            }

            @Override
            public URI completeURI() {
                return URI.create(apiUrlBase + "/graph/" + flowId.getId() + "/stage/" + completionId.getId() + "/complete");
            }

            @Override
            public URI failureURI() {
                return URI.create(apiUrlBase + "/graph/" + flowId.getId() + "/stage/" + completionId.getId() + "/fail");
            }
        };
    }

    @Override
    public CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation) {
        return requestCompletion("/graph/" + flowId.getId() + "/invokeFunction",
                req -> req.withQueryParam("functionId", functionId)
                        .withBody(data)
                        .withHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ)
                        .withHeader(METHOD_HEADER, method.toString())
                        .withHeader(CONTENT_TYPE_HEADER, headers.get(CONTENT_TYPE_HEADER).orElse(DEFAULT_CONTENT_TYPE))
                        .withHeader(FN_CODE_LOCATION, codeLocation.getLocation())
                        .withAdditionalHeaders(headers.getAll().entrySet().stream()
                                .filter((header) -> !header.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER))
                                .collect(Collectors.toMap(
                                        (headerEntry) -> USER_HEADER_PREFIX + headerEntry.getKey(),
                                        Map.Entry::getValue
                                        )
                                ))
        );
    }

    @Override
    public CompletionId completedValue(FlowId flowId, Serializable value, CodeLocation codeLocation) {
        return requestCompletionWithBody("/graph/" + flowId.getId() + "/completedValue", Function.identity(), value, codeLocation);
    }

    @Override
    public CompletionId allOf(FlowId flowId, List<CompletionId> deps, CodeLocation codeLocation) {
        return requestCompletion("/graph/" + flowId.getId() + "/allOf",
                req -> req.withQueryParam("cids", getCids(deps))
                        .withHeader(FN_CODE_LOCATION, codeLocation.getLocation()));
    }

    @Override
    public CompletionId handle(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "handle", fn, codeLocation);
    }

    @Override
    public CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation) {
        return chainThis(flowId, completionId, "exceptionally", fn, codeLocation);
    }

    @Override
    public CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable fn, CompletionId alternate, CodeLocation codeLocation) {
        return chainThisWithThat(flowId, completionId, alternate, "thenCombine", fn, codeLocation);
    }

    @Override
    public CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return requestCompletion("/graph/" + flowId.getId() + "/anyOf",
                req -> req.withQueryParam("cids", getCids(cids))
                          .withHeader(FN_CODE_LOCATION, codeLocation.getLocation()));
    }

    @Override
    public CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation) {
        return requestCompletion("/graph/" + flowId.getId() + "/delay",
                req -> req.withQueryParam("delayMs", Long.toString(l))
                        .withHeader(FN_CODE_LOCATION, codeLocation.getLocation()));
    }

    // wait for completion  -> result
    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored, long timeout, TimeUnit unit) throws TimeoutException {
        long timeoutMs = unit.toMillis(timeout);
        HttpClient.HttpRequest req = HttpClient
                .prepareGet(apiUrlBase + "/graph/" + flowId.getId() + "/stage/" + id.getId());
        if (timeoutMs > 0) {
            req = req.withQueryParam("timeoutMs", Long.toString(timeoutMs));
        }

        try (HttpClient.HttpResponse response = httpClient.execute(req)) {
            if (response.getStatusCode() == HTTP_CODE_REQUEST_TIMEOUT) {
                throw new TimeoutException("Timed out waiting getting stage from completer service");
            }
            validateSuccessful(response);

            SerUtils.ContentPart result = SerUtils.ContentPart.readFromStream(response);

            // check if the response headers indicate that the response body is an Exception/Error
            if (resultingInException(response)) {
                if (resultingFromExternalFunctionInvocation(response) ||
                        resultingFromUserException(response) ||
                        resultingFromExternallyCompletedStage(response)) {
                    Throwable userException = (Throwable) result.get();
                    throw new FlowCompletionException(userException);
                } else if (resultingFromPlatformError(response)) {
                    throw (PlatformException) result.get();
                }
            }

            return result.get();
        } catch (FlowCompletionException | TimeoutException e) {
            throw e;
        } catch (ClassNotFoundException | IOException | SerUtils.Deserializer.DeserializeException e) {
            throw new ResultSerializationException("Unable to deserialize result received from the completer service", e);
        } catch (Exception e) {
            throw new PlatformException("Request to completer service failed");
        }
    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader ignored){
        try {
            return waitForCompletion(flowId, id, ignored, 0, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // should never happen if completer's default timeout is larger than fn invocation timeout
            throw new PlatformException(e);
        }
    }

    private boolean resultingInException(HttpClient.HttpResponse response) {
        return response.getHeaderValue(RESULT_STATUS_HEADER).get().equalsIgnoreCase(RESULT_STATUS_FAILURE) ||
                response.getHeaderValue(DATUM_TYPE_HEADER).get().equalsIgnoreCase(DATUM_TYPE_ERROR);
    }

    private boolean resultingFromExternalFunctionInvocation(HttpClient.HttpResponse response) {
        return response.getHeaderValue(DATUM_TYPE_HEADER).get().equalsIgnoreCase(DATUM_TYPE_HTTP_RESP);
    }

    private boolean resultingFromExternallyCompletedStage(HttpClient.HttpResponse response) {
        return response.getHeaderValue(DATUM_TYPE_HEADER).get().equalsIgnoreCase(DATUM_TYPE_HTTP_REQ);
    }

    private boolean resultingFromPlatformError(HttpClient.HttpResponse response) {
        return response.getHeaderValue(DATUM_TYPE_HEADER).get().equalsIgnoreCase(DATUM_TYPE_ERROR);
    }

    private boolean resultingFromUserException(HttpClient.HttpResponse response) {
        return response.getHeaderValue(DATUM_TYPE_HEADER).get().equalsIgnoreCase(DATUM_TYPE_BLOB);
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
        requestTerminationHook("/graph/" + flowId.getId() + "/terminationHook", Function.identity(), code, codeLocation);
    }

    private CompletionId chainThis(FlowId flowId, CompletionId completionId, String op, Serializable fn, CodeLocation codeLocation) {
        return requestCompletionWithBody("/graph/" + flowId.getId() + "/stage/" + completionId.getId() + "/" + op, Function.identity(), fn,
                codeLocation);
    }

    private CompletionId chainThisWithThat(FlowId flowId, CompletionId currentStage, CompletionId other, String op, Serializable fn, CodeLocation codeLocation) {
        return requestCompletionWithBody("/graph/" + flowId.getId() + "/stage/" + currentStage.getId() + "/" + op,
                req -> req.withQueryParam("other", other.getId()), fn, codeLocation);
    }

    private static void validateSuccessful(HttpClient.HttpResponse response) {
        if (!isSuccessful(response)) {
            try {
                throw new PlatformException(String.format("Received unexpected response (%d) from " +
                        "completer: %s", response.getStatusCode(), response.entityAsString()));
            } catch (IOException e) {
                throw new PlatformException(String.format("Received unexpected response (%d) from " +
                        "completer. Could not read body.", response.getStatusCode()));
            }
        }
    }

    private static boolean isSuccessful(HttpClient.HttpResponse response) {
        return response.getStatusCode() == 200 || response.getStatusCode() == 201;
    }

    private CompletionId requestCompletion(String url, Function<HttpClient.HttpRequest, HttpClient.HttpRequest> fn) {
        HttpClient.HttpRequest req = fn.apply(HttpClient.preparePost(apiUrlBase + url));

        try (com.fnproject.fn.runtime.flow.HttpClient.HttpResponse resp = httpClient.execute(req)) {
            validateSuccessful(resp);
            String completionId = resp.getHeader(STAGE_ID_HEADER);
            return new CompletionId(completionId);
        } catch (PlatformException e) {
            throw e;
        } catch (Exception e) {
            throw new PlatformException("Failed to get response from completer: " + e.getMessage());
        }
    }

    private CompletionId requestCompletionWithBody(String url, Function<HttpClient.HttpRequest, HttpClient.HttpRequest> fn, Serializable ser,
                                                   CodeLocation codeLocation) {
        try {
            byte[] serBytes = SerUtils.serialize(ser);
            return requestCompletion(url, req -> fn.apply(req
                    .withHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT)
                    .withHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB)
                    .withHeader(FN_CODE_LOCATION, codeLocation.getLocation())
                    .withBody(serBytes)));
        } catch (IOException e) {
            throw new LambdaSerializationException("Failed to serialize the lambda: " + e.getMessage());
        }
    }

    private void requestTerminationHook(String url, Function<HttpClient.HttpRequest, HttpClient.HttpRequest> fn, Serializable ser,
                                        CodeLocation codeLocation) {
        try {
            byte[] serBytes = SerUtils.serialize(ser);
            HttpClient.HttpRequest req = fn.apply(
                    HttpClient.preparePost(apiUrlBase + url)
                            .withHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT)
                            .withHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB)
                            .withHeader(FN_CODE_LOCATION, codeLocation.getLocation())
                            .withBody(serBytes));
            try (com.fnproject.fn.runtime.flow.HttpClient.HttpResponse resp = httpClient.execute(req)) {
                validateSuccessful(resp);
            } catch (PlatformException e) {
                throw e;
            } catch (Exception e) {
                throw new PlatformException("Failed to get response from completer: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new LambdaSerializationException("Failed to serialize the lambda: " + e.getMessage());
        }
    }

    private static String getCids(List<CompletionId> cids) {
        Set<String> completionIds = cids.stream().map(CompletionId::getId).collect(Collectors.toSet());
        return String.join(",", completionIds);
    }


}
