package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.fnproject.fn.runtime.flow.RemoteCompleterApiClient.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests against outbound http contract for completer
public class RemoteCompleterApiClientTest {

    private final ClassLoader classLoader = getClass().getClassLoader();
    private final CodeLocation codeLocation = locationFn();
    private final Flows.SerCallable<Integer> serializableLambda = () -> 42;
    private final byte[] lambdaBytes;
    private final FlowId flowId = new FlowId("flow-id");
    private final CompletionId callerId = new CompletionId("caller-id");
    private final CompletionId parentId = new CompletionId("parent-id");
    private final CompletionId otherId = new CompletionId("other-id");
    private final CompletionId otherId2 = new CompletionId("other-id2");

    {
        try {
            lambdaBytes = SerUtils.serialize(serializableLambda);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CodeLocation locationFn() {
        return CodeLocation.fromCallerLocation(0);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private HttpClient mockHttpClient = mock(HttpClient.class, RETURNS_DEEP_STUBS);

    private ArgumentCaptor<HttpClient.HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpClient.HttpRequest.class);

    private RemoteCompleterApiClient completerClient = new RemoteCompleterApiClient("", mockHttpClient);

    private static final Throwable[] throwableStageResults = new Throwable[]{
            new RuntimeException("Woops"),
            new OutOfMemoryError("Oh no")
    };

    private static final Map<String, Throwable> platformErrorResults = new HashMap<>();

    static {
        platformErrorResults.put(ERROR_TYPE_STAGE_TIMEOUT, new StageTimeoutException("foo"));
        platformErrorResults.put(ERROR_TYPE_STAGE_INVOKE_FAILED, new StageInvokeFailedException("bar"));
        platformErrorResults.put(ERROR_TYPE_FUNCTION_TIMEOUT, new FunctionTimeoutException("baz"));
        platformErrorResults.put(ERROR_TYPE_FUNCTION_INVOKE_FAILED, new FunctionInvokeFailedException("quux"));
        platformErrorResults.put(ERROR_TYPE_STAGE_LOST, new StageLostException("xyzzy"));
        platformErrorResults.put(ERROR_TYPE_INVALID_STAGE_RESPONSE, new InvalidStageResponseException("plugh"));
        platformErrorResults.put("some-unknown-error", new PlatformException("drat"));
    }


    private InputStream serializeToStream(Object o) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(o);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
    }

    @Before
    public void setup(){
        FlowRuntimeGlobals.setCurrentCompletionId(callerId);
    }
    @After
    public void tearDown(){
        FlowRuntimeGlobals.setCurrentCompletionId(null);
    }

    @Test
    public void waitForCompletionShouldReturnFunctionResponseOnFunctionInvocation() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(RESULT_CODE_HEADER, "200");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenReturn(response);

        // When
        Object result = completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);

        // Then
        assertThat(result).isInstanceOf(HttpResponse.class);
    }

    @Test
    public void waitForCompletionShouldBlock() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(RESULT_CODE_HEADER, "200");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenAnswer(inv -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            return response;
        });

        // When
        Object result = completerClient
                .waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader, 0, TimeUnit.SECONDS);

        // Then
        assertThat(result).isInstanceOf(HttpResponse.class);
    }

    @Test(expected = TimeoutException.class)
    public void waitForCompletionWithTimeoutShouldThrowTimeoutException() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(RESULT_CODE_HEADER, "200");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenAnswer(inv -> {
            throw new TimeoutException();
        });

        // When
        completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void waitForCompletionWithoutTimeoutShouldWrapTimeoutException() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(RESULT_CODE_HEADER, "200");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenAnswer(inv -> {
            throw new TimeoutException();
        });

        // When
        try {
            completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
        } catch (PlatformException e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }
    }

    @Test
    public void waitForCompletionShouldThrowFunctionInvocationExceptionOnFailedFunctionInvocation() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_RESP);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
        response.addHeader(RESULT_CODE_HEADER, "500");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenReturn(response);

        // When
        try {
            completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
            fail("should have thrown an exception");
        } catch (FlowCompletionException e) {
            assertThat(e.getCause()).isInstanceOfAny(FunctionInvocationException.class);
            assertThat(((FunctionInvocationException) e.getCause()).getFunctionResponse().getStatusCode()).isEqualTo(500);
        }
    }

    @Test
    public void waitForCompletionShouldThrowExceptionIfValidationFails() throws Exception {
        int responseCode = 500;
        String errorResponse = "Internal server error";

        thrown.expect(PlatformException.class);
        thrown.expectMessage(String.format("Received unexpected response (%d) from completer: %s", responseCode, errorResponse));

        HttpClient.HttpResponse invalidResponse = mock(HttpClient.HttpResponse.class);
        when(invalidResponse.getStatusCode()).thenReturn(responseCode);
        when(invalidResponse.entityAsString()).thenReturn(errorResponse);
        when((Object) mockHttpClient.execute(any())).thenReturn(invalidResponse);

        completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
    }

    @Test
    public void waitForCompletionThrowsPlatformExceptionIfErrorInHeader() throws Exception {
        for (Map.Entry<String, Throwable> item : platformErrorResults.entrySet()) {
            String errorHeader = item.getKey();
            Throwable exception = item.getValue();

            thrown.expect(exception.getClass());
            thrown.expectMessage(exception.getMessage());

            HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
            response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_ERROR);
            response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
            response.addHeader(CONTENT_TYPE_HEADER, "text/plain");
            response.addHeader(ERROR_TYPE_HEADER, errorHeader);
            response.setEntity(IOUtils.toInputStream(exception.getMessage(), "utf-8"));

            when((Object) mockHttpClient.execute(any())).thenReturn(response);

            completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
        }
    }

    @Test
    public void waitForCompletionThrowsCompletionExceptionIfFailedInHeader() throws Exception {
        for (Throwable stageResult : throwableStageResults) {
            thrown.expect(FlowCompletionException.class);
            thrown.expectMessage(stageResult.getMessage());

            HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
            response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB);
            response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
            response.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT);
            response.setEntity(serializeToStream(stageResult));
            when((Object) mockHttpClient.execute(any())).thenReturn(response);

            completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
        }
    }


    @Test
    public void throwsResultSerializationExceptionIfResultCannotBeDeserialized() throws Exception {
        thrown.expect(ResultSerializationException.class);
        thrown.expectMessage("Unable to deserialize result received from the completer service");

        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT);
        response.setEntity(new ByteArrayInputStream("".getBytes()));

        when((Object) mockHttpClient.execute(any())).thenReturn(response);

        completerClient.waitForCompletion(new FlowId("1"), new CompletionId("2"), classLoader);
    }

    @Test
    public void throwsLambdaSerializationExceptionIfNonSerializableLambdaIsUsed() {
        thrown.expect(LambdaSerializationException.class);
        thrown.expectMessage("Failed to serialize the lambda");

        Optional<String> unserializableValue = Optional.of("hello");
        RemoteCompleterApiClient completerClient = new RemoteCompleterApiClient("", mockHttpClient);
        Flows.SerCallable<Optional<String>> unserializableLambda = () -> unserializableValue;

        completerClient.supply(new FlowId("flow-id"), unserializableLambda, codeLocation);
    }

    @Test
    public void throwsPlatformExceptionIfFailedToCreateFlow() throws Exception {
        thrown.expect(PlatformCommunicationException.class);
        thrown.expectMessage("Failed to create flow");

        when((Object) mockHttpClient.execute(any())).thenThrow(new RuntimeException("Connection refused"));

        RemoteCompleterApiClient completerClient = new RemoteCompleterApiClient("", mockHttpClient);
        completerClient.createFlow("function-id");
    }

    @Test
    public void throwsPlatformExceptionIfFailedToRequestCompletion() throws Exception {
        thrown.expect(PlatformException.class);
        thrown.expectMessage("Failed to get response from completer");

        RemoteCompleterApiClient completerClient = new RemoteCompleterApiClient("", mockHttpClient);
        Flows.SerCallable<Integer> serializableLambda = () -> 42;
        when((Object) mockHttpClient.execute(any())).thenThrow(new RuntimeException("Connection refused"));

        completerClient.supply(new FlowId("flow-id"), serializableLambda, codeLocation);
    }

    @Test
    public void callsCreateFlowOK() throws Exception {
        HttpClient.HttpResponse stageResponse = new HttpClient.HttpResponse(200);
        stageResponse.addHeader(FLOW_ID_HEADER, "flow-id");
        when(mockHttpClient.execute(any())).thenReturn(stageResponse);
        FlowId flowId = completerClient.createFlow("funcy");

        assertThat(flowId.getId()).isEqualTo("flow-id");

        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph");
        assertThat(req.query).contains(entry("functionId","funcy"));
    }
    @Test
    public void callsSupplyOK() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.supply(flowId, serializableLambda, codeLocation), "supply");
    }

    @Test
    public void callsThenApplyOk() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.thenApply(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/thenApply");
    }

    @Test
    public void callsThenAcceptOk() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.thenAccept(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/thenAccept");
    }

    @Test
    public void callsThenCombine() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.thenCombine(flowId, parentId, serializableLambda, otherId, codeLocation), "stage/parent-id/thenCombine");

        assertThat(req.query).contains(entry("other", "other-id"));
    }


    @Test
    public void callsThenCompose() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.thenCompose(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/thenCompose");
    }

    @Test
    public void callsWhenComplete() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.whenComplete(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/whenComplete");
    }

    @Test
    public void callsThenAccept() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.thenAccept(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/thenAccept");
    }

    @Test
    public void callsThenRun() throws Exception {
        givenValidStageResponse();

        verifyStageCreatedOnGraph(completerClient.thenRun(flowId, parentId, serializableLambda, codeLocation), "stage/parent-id/thenRun");
    }

    @Test
    public void callsAcceptEither() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.acceptEither(flowId, parentId, otherId, serializableLambda, codeLocation), "stage/parent-id/acceptEither");

        assertThat(req.query).contains(entry("other", "other-id"));
    }

    @Test
    public void callsApplyToEither() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.applyToEither(flowId, parentId, otherId, serializableLambda, codeLocation), "stage/parent-id/applyToEither");

        assertThat(req.query).contains(entry("other", "other-id"));
    }

    @Test
    public void callsThenAcceptBoth() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.thenAcceptBoth(flowId, parentId, otherId, serializableLambda, codeLocation), "stage/parent-id/thenAcceptBoth");

        assertThat(req.query).contains(entry("other", "other-id"));
    }


    @Test
    public void callsHandle() throws Exception {
        givenValidStageResponse();
        verifyStageCreatedOnGraph(completerClient.handle(flowId, parentId,serializableLambda, codeLocation), "stage/parent-id/handle");
    }

    @Test
    public void callsExceptionally() throws Exception {
        givenValidStageResponse();
        verifyStageCreatedOnGraph(completerClient.exceptionally(flowId, parentId,serializableLambda, codeLocation), "stage/parent-id/exceptionally");
    }


    @Test
    public void createsInvokeFunction() throws Exception {
        givenValidStageResponse();
        Map<String, String> headers = new HashMap<>();
        headers.put("Header-1", "h1");
        headers.put("Header-2", "h2");


        CompletionId completion = completerClient.invokeFunction(flowId, "funcy", "data".getBytes(), HttpMethod.GET, Headers.fromMap(headers), codeLocation);

        String url = String.format("/graph/%s/invokeFunction", flowId.getId());

        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        assertThat(completion.getId()).isEqualTo("stage");
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.query).contains(entry("functionId", "funcy"));
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo(url);
        assertThat(req.headers).contains(
                entry(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ),
                entry(USER_HEADER_PREFIX + "Header-1", "h1"),
                entry(USER_HEADER_PREFIX + "Header-2", "h2"),
                entry(CONTENT_TYPE_HEADER, DEFAULT_CONTENT_TYPE),
                entry(FN_CODE_LOCATION, codeLocation.getLocation()),
                entry(CALLER_ID_HEADER,callerId.getId()));


    }


    @Test
    public void callsCompletedValueSuccess() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.completedValue(flowId, true, serializableLambda, codeLocation), "completedValue");

        assertThat(req.headers).contains(entry(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS));

    }

    @Test
    public void callsCompletedValueFailure() throws Exception {
        givenValidStageResponse();

        HttpClient.HttpRequest req = verifyStageCreatedOnGraph(completerClient.completedValue(flowId, false, serializableLambda, codeLocation), "completedValue");

        assertThat(req.headers).contains(entry(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE));

    }


    @Test
    public void callsAllOf() throws Exception {
        givenValidStageResponse();

        CompletionId completion = completerClient.allOf(flowId, Arrays.asList(otherId,otherId2), codeLocation);

        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        assertThat(completion.getId()).isEqualTo("stage");
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.query).contains(entry("cids", "other-id,other-id2"));
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph/flow-id/allOf");
        assertThat(req.headers).contains(
                entry(FN_CODE_LOCATION, codeLocation.getLocation()));
    }

    @Test
    public void callsAnyOf() throws Exception {
        givenValidStageResponse();

        CompletionId completion = completerClient.anyOf(flowId, Arrays.asList(otherId,otherId2), codeLocation);

        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        assertThat(completion.getId()).isEqualTo("stage");
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.query).contains(entry("cids", "other-id,other-id2"));
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph/flow-id/anyOf");
        assertThat(req.headers).contains(
                entry(FN_CODE_LOCATION, codeLocation.getLocation()),
                entry(CALLER_ID_HEADER,callerId.getId()));

    }

    @Test
    public void callsDelay() throws Exception {
        givenValidStageResponse();

        CompletionId completion = completerClient.delay(flowId, 3141, codeLocation);

        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        assertThat(completion.getId()).isEqualTo("stage");
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.query).contains(entry("delayMs", "3141"));
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph/flow-id/delay");
        assertThat(req.headers).contains(
                entry(FN_CODE_LOCATION, codeLocation.getLocation()),
                entry(CALLER_ID_HEADER,callerId.getId()));
    }

    @Test
    public void callsCommit() throws Exception{
        HttpClient.HttpResponse stageResponse = new HttpClient.HttpResponse(200);
        when(mockHttpClient.execute(any())).thenReturn(stageResponse);

        completerClient.commit(flowId);
        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph/flow-id/commit");
    }


    @Test
    public void callsAddTerminationHook() throws Exception{
        HttpClient.HttpResponse stageResponse = new HttpClient.HttpResponse(200);
        when(mockHttpClient.execute(any())).thenReturn(stageResponse);

        completerClient.addTerminationHook(flowId,serializableLambda,codeLocation);
        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo("/graph/flow-id/terminationHook");
        assertThat(req.headers).contains(
                entry(FN_CODE_LOCATION, codeLocation.getLocation()),
                entry(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB),
                entry(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT),
                entry(CALLER_ID_HEADER,callerId.getId()));
        assertThat(req.bodyBytes).isEqualTo(lambdaBytes);
    }

    @Test
    public void emptyFailedResponseDoesNotThrowNPE() throws Exception {
        givenFailedResponseWithoutBody();

        try {
            verifyStageCreatedOnGraph(completerClient.supply(flowId, serializableLambda, codeLocation), "supply");
        } catch (PlatformException e) {
            assertThat(e.getCause()).isNull();
            assertThat(e.getMessage().toLowerCase()).contains("empty body");
        }
    }

    private HttpClient.HttpRequest verifyStageCreatedOnGraph(CompletionId id, String op) throws IOException {
        String url = String.format("/graph/%s/%s", flowId.getId(), op);
        verify(mockHttpClient, times(1)).execute(reqCaptor.capture());
        verifyNoMoreInteractions(mockHttpClient);

        assertThat(id.getId()).isEqualTo("stage");
        HttpClient.HttpRequest req = reqCaptor.getValue();
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.url).isEqualTo(url);
        assertThat(req.headers).contains(
                entry(FN_CODE_LOCATION, codeLocation.getLocation()),
                entry(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB),
                entry(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT),
                entry(CALLER_ID_HEADER,callerId.getId()));
        assertThat(req.bodyBytes).isEqualTo(lambdaBytes);
        return req;
    }

    private void givenValidStageResponse() throws IOException {
        HttpClient.HttpResponse stageResponse = new HttpClient.HttpResponse(200);
        stageResponse.addHeader(STAGE_ID_HEADER, "stage");
        when(mockHttpClient.execute(any())).thenReturn(stageResponse);
    }

    private void givenFailedResponseWithoutBody() throws IOException {
        HttpClient.HttpResponse empty = new HttpClient.HttpResponse(500).setEntity(null);
        when(mockHttpClient.execute(any())).thenReturn(empty);
    }

}
