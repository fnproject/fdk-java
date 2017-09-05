package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.cloudthreads.*;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class CloudCompleterApiClientTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private HttpClient mockHttpClient = mock(HttpClient.class, RETURNS_DEEP_STUBS);

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
        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        Object result = completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());

        // Then
        assertThat(result).isInstanceOf(HttpResponse.class);
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
        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        try {
            Object result = completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
        } catch (CloudCompletionException e) {
            assertThat(e.getCause()).isInstanceOfAny(FunctionInvocationException.class);
            assertThat(((FunctionInvocationException)e.getCause()).getFunctionResponse().getStatusCode()).isEqualTo(500);
        }
    }

    @Test
    public void waitForCompletionShouldReturnHttpRequestOnExternallyCompletable() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_SUCCESS);
        response.addHeader(REQUEST_METHOD_HEADER, "POST");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenReturn(response);

        // When
        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        Object result = completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());

        // Then
        assertThat(result).isInstanceOf(HttpRequest.class);
    }


    @Test
    public void waitForCompletionShouldThrowExternalCompletionExceptionOnFailedExternalFuture() throws Exception {
        // Given
        HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
        response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ);
        response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
        response.addHeader(REQUEST_METHOD_HEADER, "POST");
        response.addHeader(CONTENT_TYPE_HEADER, "application/json");
        response.addHeader(USER_HEADER_PREFIX + "Custom-Header", "myValue");
        response.setEntity(IOUtils.toInputStream("{ \"some\": \"json\" }", "utf-8"));
        when((Object) mockHttpClient.execute(any())).thenReturn(response);

        // When
        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        try {
            completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
            fail("Should have thrown an exception");
        } catch (CloudCompletionException e) {
            // Just as with thrown exceptions, the ECEx is wrapped in a CCEx on .get
            assertThat(e.getCause()).isInstanceOfAny(ExternalCompletionException.class);
            assertThat(((ExternalCompletionException)e.getCause()).getExternalRequest().getMethod()).isEqualTo(HttpMethod.POST);
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

        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
    }

    @Test
    public void waitForCompletionThrowsPlatformExceptionIfErrorInHeader() throws Exception {
        for (Map.Entry<String, Throwable> item: platformErrorResults.entrySet()) {
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

            CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
            completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
        }
    }

    @Test
    public void waitForCompletionThrowsCompletionExceptionIfFailedInHeader() throws Exception {
        for(Throwable stageResult : throwableStageResults) {
            thrown.expect(CloudCompletionException.class);
            thrown.expectMessage(stageResult.getMessage());

            HttpClient.HttpResponse response = new HttpClient.HttpResponse(200);
            response.addHeader(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB);
            response.addHeader(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);
            response.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT);
            response.setEntity(serializeToStream(stageResult));
            when((Object) mockHttpClient.execute(any())).thenReturn(response);

            CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
            completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
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

        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        completerClient.waitForCompletion(new ThreadId("1"), new CompletionId("2"), getClass().getClassLoader());
    }

    @Test
    public void throwsLambdaSerializationExceptionIfNonSerializableLambdaIsUsed() {
        thrown.expect(LambdaSerializationException.class);
        thrown.expectMessage("Failed to serialize the lambda");

        Optional<String> unserializableValue = Optional.of("hello");
        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        CloudThreads.SerCallable<Optional<String>> unserializableLambda = () -> unserializableValue;

        completerClient.supply(new ThreadId("thread-id"), unserializableLambda);
    }

    @Test
    public void throwsPlatformExceptionIfFailedToCreateThread() throws Exception {
        thrown.expect(PlatformCommunicationException.class);
        thrown.expectMessage("Failed to create cloud thread: Connection refused");

        when((Object) mockHttpClient.execute(any())).thenThrow(new RuntimeException("Connection refused"));

        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        completerClient.createThread("function-id");
    }

    @Test
    public void throwsPlatformExceptionIfFailedToRequestCompletion() throws Exception {
        thrown.expect(PlatformException.class);
        thrown.expectMessage("Failed to get response from completer: Connection refused");

        CloudCompleterApiClient completerClient = new CloudCompleterApiClient("", mockHttpClient);
        CloudThreads.SerCallable<Integer> serializableLambda = () -> 42;
        when((Object) mockHttpClient.execute(any())).thenThrow(new RuntimeException("Connection refused"));

        completerClient.supply(new ThreadId("thread-id"), serializableLambda);
    }
}
