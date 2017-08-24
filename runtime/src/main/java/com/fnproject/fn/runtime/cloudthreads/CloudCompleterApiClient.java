package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.*;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fnproject.fn.runtime.cloudthreads.HttpClient;

public class CloudCompleterApiClient implements CompleterClient {
    private transient final HttpClient httpClient;
    private final String apiUrlBase;

    // TODO: move these to SerUtils

    private static final String HEADER_PREFIX = "FnProject-";
    static final String THREAD_ID_HEADER = HEADER_PREFIX + "ThreadID";
    static final String STAGE_ID_HEADER = HEADER_PREFIX + "StageID";
    static final String METHOD_HEADER = HEADER_PREFIX + "Method";
    static final String USER_HEADER_PREFIX = HEADER_PREFIX + "Header-";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_TYPE_JAVA_OBJECT = "application/java-serialized-object";
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public static final String DATUM_TYPE_HEADER = HEADER_PREFIX + "DatumType";
    public static final String DATUM_TYPE_BLOB = "blob";
    public static final String DATUM_TYPE_EMPTY = "empty";
    public static final String DATUM_TYPE_ERROR = "error";
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

    protected CloudCompleterApiClient(String apiUrlBase, HttpClient httpClient) {
        this.apiUrlBase = Objects.requireNonNull(apiUrlBase);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public ThreadId createThread(String functionId) {
        HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/graph?functionId=" + functionId);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            validateSuccessful(resp);
            return new ThreadId(resp.getHeader(THREAD_ID_HEADER));
        } catch (Exception e) {
            throw new PlatformCommunicationException("Failed to create cloud thread: " + e.getMessage());
        }
    }

    @Override
    public CompletionId supply(ThreadId threadId, Serializable supplier) {
        return requestCompletionWithBody("/graph/" + threadId.getId() + "/supply", Function.identity(), supplier);
    }

    @Override
    public CompletionId thenApply(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "thenApply", fn);
    }

    @Override
    public CompletionId thenCompose(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "thenCompose", fn);
    }

    @Override
    public CompletionId whenComplete(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "whenComplete", fn);
    }

    @Override
    public CompletionId thenAccept(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "thenAccept", fn);
    }

    @Override
    public CompletionId thenRun(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "thenRun", fn);
    }

    @Override
    public CompletionId acceptEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return chainThisWithThat(threadId, completionId, alternate, "acceptEither", fn);
    }

    @Override
    public CompletionId applyToEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return chainThisWithThat(threadId, completionId, alternate, "applyToEither", fn);
    }

    @Override
    public CompletionId thenAcceptBoth(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return chainThisWithThat(threadId, completionId, alternate, "thenAcceptBoth", fn);
    }

    @Override
    public ExternalCompletion createExternalCompletion(ThreadId threadId) {
        CompletionId completionId = requestCompletion("/graph/" + threadId.getId() + "/externalCompletion", Function.identity());
        return new ExternalCompletion() {
            @Override
            public CompletionId completionId() {
                return completionId;
            }

            @Override
            public URI completeURI() {
                return URI.create(apiUrlBase + "/graph/" + threadId.getId() + "/stage/" + completionId.getId() + "/complete");
            }

            @Override
            public URI failureURI() {
                return URI.create(apiUrlBase + "/graph/" + threadId.getId() + "/stage/" + completionId.getId() + "/fail");
            }
        };
    }

    @Override
    public CompletionId invokeFunction(ThreadId threadId, String functionId, byte[] data, HttpMethod method, Headers headers) {
        return requestCompletion("/graph/" + threadId.getId() + "/invokeFunction",
                req -> req.withQueryParam("functionId", functionId)
                        .withBody(data)
                        .withHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ)
                        .withHeader(METHOD_HEADER, method.toString())
                        .withHeader(CONTENT_TYPE_HEADER, headers.get(CONTENT_TYPE_HEADER).orElse(DEFAULT_CONTENT_TYPE))
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
    public CompletionId completedValue(ThreadId threadId, Serializable value) {
        return requestCompletionWithBody("/graph/" + threadId.getId() + "/completedValue", Function.identity(), value);
    }

    @Override
    public CompletionId allOf(ThreadId threadId, List<CompletionId> deps) {
        return requestCompletion("/graph/" + threadId.getId() + "/allOf",
                req -> req.withQueryParam("cids", getCids(deps)));
    }

    @Override
    public CompletionId handle(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "handle", fn);
    }

    @Override
    public CompletionId exceptionally(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return chainThis(threadId, completionId, "exceptionally", fn);
    }

    @Override
    public CompletionId thenCombine(ThreadId threadId, CompletionId completionId, Serializable fn, CompletionId alternate) {
        return chainThisWithThat(threadId, completionId, alternate, "thenCombine", fn);
    }

    @Override
    public CompletionId anyOf(ThreadId threadId, List<CompletionId> cids) {
        return requestCompletion("/graph/" + threadId.getId() + "/anyOf",
                req -> req.withQueryParam("cids", getCids(cids)));
    }

    @Override
    public CompletionId delay(ThreadId threadId, long l) {
        return requestCompletion("/graph/" + threadId.getId() + "/delay",
                req -> req.withQueryParam("delayMs", Long.toString(l)));
    }

    // wait for completion  -> result
    @Override
    public Object waitForCompletion(ThreadId threadId, CompletionId id) {
        while (true) {
            long time = System.currentTimeMillis();
            try (HttpClient.HttpResponse response = httpClient.execute(HttpClient
                    .prepareGet(apiUrlBase + "/graph/" + threadId.getId() + "/stage/" + id.getId()))
            ) {
                if (response.getStatusCode() == HTTP_CODE_REQUEST_TIMEOUT) {
                    long delay = Math.max(0, MAX_POLL_INTERVAL_MS - (System.currentTimeMillis() - time));
                    Thread.sleep(delay);
                    continue;
                }
                validateSuccessful(response);

                String datumType = response.getHeaderValue(DATUM_TYPE_HEADER).orElseThrow(() ->
                        new PlatformException("Request to completer service did not supply " + DATUM_TYPE_HEADER + " header"));

                SerUtils.ContentPart result = SerUtils.ContentPart.readFromStream(response);

                // check if the response headers indicate that the response body is an Exception/Error
                // TODO: Check whether we're going to throw an exception then build and throw
                // TODO: Add comment to API doc saying that error datum type responses always have failure result status
                if (resultingInException(response)) {
                    if (resultingFromExternalFunctionInvocation(response)) {
                        throw new FunctionInvocationException((com.fnproject.fn.api.cloudthreads.HttpResponse) result.get());
                    } else if (resultingFromExternallyCompletedStage(response)) {
                        throw new ExternalCompletionException((com.fnproject.fn.api.cloudthreads.HttpRequest) result.get());
                    } else if (resultingFromUserException(response)) {
                        Throwable userException = (Throwable) result.get();
                        throw new CloudCompletionException(userException);
                    } else if (resultingFromPlatformError(response)) {
                        throw (PlatformException) result.get();
                    }
                }

                return result.get();
            } catch (CloudCompletionException | FunctionInvocationException | ExternalCompletionException e) {
                throw e;
            } catch (ClassNotFoundException | IOException | SerUtils.Deserializer.DeserializeException e) {
                throw new ResultSerializationException("Unable to deserialize result received from the completer service", e);
            } catch (Exception e) {
                throw new PlatformException("Request to completer service failed");
            }
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

    public void commit(ThreadId threadId) {
        try (HttpClient.HttpResponse response = httpClient.execute(httpClient.preparePost(apiUrlBase + "/graph/" + threadId.getId() + "/commit"))) {
            validateSuccessful(response);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    private CompletionId chainThis(ThreadId threadId, CompletionId completionId, String op, Serializable fn) {
        return requestCompletionWithBody("/graph/" + threadId.getId() + "/stage/" + completionId.getId() + "/" + op, Function.identity(), fn);
    }

    private CompletionId chainThisWithThat(ThreadId threadId, CompletionId currentStage, CompletionId other, String op, Serializable fn) {
        return requestCompletionWithBody("/graph/" + threadId.getId() + "/stage/" + currentStage.getId() + "/" + op,
                req -> req.withQueryParam("other", other.getId()), fn);
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

        try (com.fnproject.fn.runtime.cloudthreads.HttpClient.HttpResponse resp = httpClient.execute(req)) {
            validateSuccessful(resp);
            String completionId = resp.getHeader(STAGE_ID_HEADER);
            return new CompletionId(completionId);
        } catch (PlatformException e) {
            throw e;
        } catch (Exception e) {
            throw new PlatformException("Failed to get response from completer: " + e.getMessage());
        }
    }

    private CompletionId requestCompletionWithBody(String url, Function<HttpClient.HttpRequest, HttpClient.HttpRequest> fn, Serializable ser) {
        try {
            byte[] serBytes = SerUtils.serialize(ser);
            return requestCompletion(url, req -> fn.apply(req
                    .withHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT)
                    .withHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB)
                    .withBody(serBytes)));
        } catch(IOException e) {
            throw new LambdaSerializationException("Failed to serialize the lambda: " + e.getMessage());
        }
    }

    private static String getCids(List<CompletionId> cids) {
        Set<String> completionIds = cids.stream().map(CompletionId::getId).collect(Collectors.toSet());
        return String.join(",", completionIds);
    }


}
