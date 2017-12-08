package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.FlowCompletionException;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.HttpMethod;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.progress.ThreadSafeMockingProgress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class RemoteFlowApiClientTest {

    private HttpClient mockHttpClient = mock(HttpClient.class, RETURNS_DEEP_STUBS);
    private BlobStoreClient blobStoreClient = mock(BlobStoreClient.class);
    private RemoteFlowApiClient completerClient = new RemoteFlowApiClient("", blobStoreClient, mockHttpClient);
    private ObjectMapper objectMapper = new ObjectMapper();
    private final Flows.SerCallable<Integer> serializableLambda = () -> 42;
    private final CodeLocation codeLocation = locationFn();
    private final byte[] lambdaBytes;
    private final String testFlowId = "TEST";
    private final String testStageId = "STAGEID";

    {
        try {
            lambdaBytes = serializeObject(serializableLambda);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        FlowRuntimeGlobals.setCurrentCompletionId(new CompletionId("CallerId"));
    }

    @Test
    public void createFlow() throws Exception {
        // Given
        String functionId = "functionId";

        HttpClient.HttpResponse response = responseWithCreateGraphResponse(testFlowId);

        when(mockHttpClient.execute(requestContainingFunctionId(functionId))).thenReturn(response);

        // When
        FlowId flowId = completerClient.createFlow(functionId);

        // Then
        assertNotNull(flowId);
        assertEquals(flowId.getId(), testFlowId);
    }

    @Test
    public void supply() throws Exception {
        // Given
        String contentType = "application/java-serialized-object";
        String testBlobId = "BLOBID";
        BlobResponse blobResponse = makeBlobResponse(lambdaBytes, contentType, testBlobId);
        when(blobStoreClient.writeBlob(testFlowId, lambdaBytes, contentType)).thenReturn(blobResponse);

        HttpClient.HttpResponse response = responseWithAddStageResponse(testFlowId, testStageId);
        when(mockHttpClient.execute(requestContainingAddStageRequest(blobResponse.blobId, Collections.emptyList()))).thenReturn(response);

        // When
        CompletionId completionId = completerClient.supply(new FlowId(testFlowId), serializableLambda, codeLocation);

        // Then
        assertNotNull(completionId);
        assertEquals(completionId.getId(), testStageId);
    }

    @Test
    public void invokeFunctionNormally() throws Exception {
        // Given
        String testFunctionId = "TESTFUNCTION";
        String blobid = "BLOBID";
        byte[] invokeBody = "INPUTDATA".getBytes();
        String contentType = "text/plain";

        BlobResponse blobResponse = makeBlobResponse(invokeBody, contentType, blobid);
        when(blobStoreClient.writeBlob(eq(testFlowId), eq(invokeBody), eq(contentType))).thenReturn(blobResponse);

        HttpClient.HttpResponse response = responseWithAddStageResponse(testFlowId, testStageId);
        when(mockHttpClient.execute(requestContainingAddInvokeFunctionStageRequest(testFunctionId, APIModel.HTTPMethod.Post))).thenReturn(response);

        // When
        CompletionId completionId = completerClient.invokeFunction(new FlowId(testFlowId), testFunctionId, invokeBody, HttpMethod.POST, Headers.fromMap(Collections.singletonMap("Content-type", contentType)), CodeLocation.unknownLocation());

        // Then
        assertNotNull(completionId);
        assertEquals(completionId.getId(), testStageId);
    }

    @Test
    public void invokeFunctionWithInvalidFunctionId() throws Exception {
        // Given
        String testFunctionId = "INVALIDFUNCTIONID";
        byte[] invokeBody = "INPUTDATA".getBytes();
        String contentType = "text/plain";

        BlobResponse blobResponse = makeBlobResponse(invokeBody, contentType, "BLOBID");
        when(blobStoreClient.writeBlob(testFlowId, invokeBody, contentType)).thenReturn(blobResponse);

        when(mockHttpClient.execute(requestContainingAddInvokeFunctionStageRequest(testFunctionId, APIModel.HTTPMethod.Post))).thenReturn(new HttpClient.HttpResponse(400));

        // Then
        thrown.expect(PlatformCommunicationException.class);
        thrown.expectMessage("Failed to add stage");

        // When
        completerClient.invokeFunction(new FlowId(testFlowId), testFunctionId, invokeBody, HttpMethod.POST, Headers.fromMap(Collections.singletonMap("Content-type", contentType)), locationFn());
    }

    @Test
    public void waitForCompletionNormally() throws Exception {
        // Given
        String testBlobId = "BLOBID";
        String testContentType = "application/java-serialized-object";

        APIModel.Blob blob = new APIModel.Blob();
        blob.blobId = testBlobId;
        blob.contentType = testContentType;
        APIModel.CompletionResult completionResult = APIModel.CompletionResult.success(APIModel.BlobDatum.fromBlob(blob));

        when(mockHttpClient.execute(requestForAwaitStageResult())).thenReturn(responseWithAwaitStageResponse(completionResult));
        when(blobStoreClient.readBlob(eq(testFlowId), eq("BLOBID"), any(), eq("application/java-serialized-object"))).thenReturn(1);

        // When
        Object result = completerClient.waitForCompletion(new FlowId(testFlowId), new CompletionId(testStageId), null);

        // Then
        assertEquals(result, 1);
    }

    @Test
    public void waitForCompletionOnExceptionallyCompletedStage() throws Exception {
        // Given
        String testBlobId = "BLOBID";
        String testContentType = "application/java-serialized-object";

        APIModel.Blob blob = new APIModel.Blob();
        blob.blobId = testBlobId;
        blob.contentType = testContentType;
        APIModel.CompletionResult completionResult = APIModel.CompletionResult.failure(APIModel.BlobDatum.fromBlob(blob));

        when(mockHttpClient.execute(requestForAwaitStageResult())).thenReturn(responseWithAwaitStageResponse(completionResult));

        class MyException extends RuntimeException {}

        MyException myException = new MyException();

        when(blobStoreClient.readBlob(eq(testFlowId), eq(testBlobId), any(), eq(testContentType))).thenReturn(myException);

        // Then
        thrown.expect(new BaseMatcher<Object>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("an exception of type FlowCompletionException, containing a cause of type MyException");
            }

            @Override
            public boolean matches(Object o) {
                if(o instanceof FlowCompletionException) {
                    FlowCompletionException flowCompletionException = (FlowCompletionException) o;
                    if(flowCompletionException.getCause().equals(myException)) {
                        return true;
                    }
                }
                return false;
            }
        });

        // When
        completerClient.waitForCompletion(new FlowId(testFlowId), new CompletionId(testStageId), null);
    }

    @Test
    public void waitForCompletionUnknownStage() throws Exception {
        // Given
        when(mockHttpClient.execute(requestForAwaitStageResult())).thenReturn(new HttpClient.HttpResponse(404));

        // Then
        thrown.expect(PlatformCommunicationException.class);
        thrown.expectMessage(contains("unexpected response"));

        // When
        completerClient.waitForCompletion(new FlowId(testFlowId), new CompletionId(testStageId), null);
    }

    private BlobResponse makeBlobResponse(byte[] invokeBody, String contentType, String blobid) {
        BlobResponse blobResponse = new BlobResponse();
        blobResponse.blobId = blobid;
        blobResponse.blobLength = Long.valueOf(invokeBody.length);
        blobResponse.contentType = contentType;
        return blobResponse;
    }

    private HttpClient.HttpResponse responseWithAwaitStageResponse(APIModel.CompletionResult result) throws IOException {
        APIModel.AwaitStageResponse awaitStageResponse = new APIModel.AwaitStageResponse();
        awaitStageResponse.result = result;
        return responseWithJSONBody(awaitStageResponse);
    }

    private HttpClient.HttpResponse responseWithAddStageResponse(String flowId, String stageId) throws IOException {
        APIModel.AddStageResponse addStageResponse = new APIModel.AddStageResponse();
        addStageResponse.flowId = flowId;
        addStageResponse.stageId = stageId;
        return responseWithJSONBody(addStageResponse);
    }

    private HttpClient.HttpResponse responseWithJSONBody(Object body) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        objectMapper.writeValue(byteArrayOutputStream, body);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.setEntity(byteArrayInputStream);
        return response;
    }

    private HttpClient.HttpResponse responseWithCreateGraphResponse(String flowId) throws IOException {
        APIModel.CreateGraphResponse createGraphResponse = new APIModel.CreateGraphResponse();
        createGraphResponse.flowId = flowId;
        return responseWithJSONBody(createGraphResponse);
    }

    HttpClient.HttpRequest requestForAwaitStageResult() {
        ArgumentMatcher<HttpClient.HttpRequest> argumentMatcher = httpRequest -> {
            if(httpRequest.method != "GET") {
                System.err.println("Expecting a GET request, got " + httpRequest.method);
                return false;
            }
            return true;
        };
        ThreadSafeMockingProgress.mockingProgress().getArgumentMatcherStorage().reportMatcher(argumentMatcher);
        return null;
    }

    HttpClient.HttpRequest requestContainingFunctionId(String functionId) {
        ArgumentMatcher<HttpClient.HttpRequest> argumentMatcher = httpRequest -> {
            if(httpRequest.method != "POST") {
                System.err.println("Expecting a POST request, got " + httpRequest.method);
                return false;
            }
            try {
                APIModel.CreateGraphRequest createGraphRequest = objectMapper.readValue(new String(httpRequest.bodyBytes), APIModel.CreateGraphRequest.class);
                if ((createGraphRequest.functionId != null) && (createGraphRequest.functionId.equals(functionId))) {
                    return true;
                }
                System.err.println("Request body doesn't contain an CreateGraphRequest with matching functionId field");
                return false;
            } catch (IOException e) {
                System.err.println("Request body doesn't contain an CreateGraphRequest");
                return false;
            }
        };
        ThreadSafeMockingProgress.mockingProgress().getArgumentMatcherStorage().reportMatcher(argumentMatcher);
        return null;
    }

    HttpClient.HttpRequest requestContainingAddStageRequest(String blobId, List<String> dependencies) {
        ArgumentMatcher<HttpClient.HttpRequest> argumentMatcher = httpRequest -> {
            if(httpRequest.method != "POST") {
                System.err.println("Expecting a POST request, got " + httpRequest.method);
                return false;
            }
            try {
                APIModel.AddStageRequest addStageRequest = objectMapper.readValue(new String(httpRequest.bodyBytes), APIModel.AddStageRequest.class);
                if (    (addStageRequest.closure != null) &&
                        (addStageRequest.closure.blobId.equals(blobId)) &&
                        (addStageRequest.deps.equals(dependencies))) {
                    return true;
                }
                System.err.println("Request body doesn't contain an AddStageRequest with closure with matching blobId field");
                return false;
            } catch (IOException e) {
                System.err.println("Request body doesn't contain an AddStageRequest");
                return false;
            }
        };
        ThreadSafeMockingProgress.mockingProgress().getArgumentMatcherStorage().reportMatcher(argumentMatcher);
        return null;
    }

    HttpClient.HttpRequest requestContainingAddInvokeFunctionStageRequest(String functionId, APIModel.HTTPMethod httpMethod) {
        ArgumentMatcher<HttpClient.HttpRequest> argumentMatcher = httpRequest -> {
            if(httpRequest.method != "POST") {
                System.err.println("Expecting a POST request, got " + httpRequest.method);
                return false;
            }
            try {
                APIModel.AddInvokeFunctionStageRequest addInvokeFunctionStageRequest = objectMapper.readValue(new String(httpRequest.bodyBytes), APIModel.AddInvokeFunctionStageRequest.class);
                if ((addInvokeFunctionStageRequest.functionId != null) &&
                        (addInvokeFunctionStageRequest.functionId.equals(functionId)) &&
                        (addInvokeFunctionStageRequest.arg.method.equals(httpMethod))) {
                    return true;
                } else {
                    System.err.println("Request body doesn't contain a matching AddInvokeFunctionStageRequest");
                }
                return false;
            } catch (IOException e) {
                System.err.println("Request body doesn't contain an AddInvokeFunctionStageRequest");
                return false;
            }
        };
        ThreadSafeMockingProgress.mockingProgress().getArgumentMatcherStorage().reportMatcher(argumentMatcher);
        return null;
    }

    private byte[] serializeObject(Object value) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(value);
        return byteArrayOutputStream.toByteArray();
    }

    private static CodeLocation locationFn() {
        return CodeLocation.fromCallerLocation(0);
    }
}