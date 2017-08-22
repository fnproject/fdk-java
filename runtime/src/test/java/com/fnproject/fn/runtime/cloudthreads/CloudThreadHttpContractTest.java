package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.cloudthreads.CloudThreads;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.TestSerUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import static com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CloudThreadHttpContractTest {
    public CloudCompleterApiClient client;
    private final String THREAD_ID = UUID.randomUUID().toString();
    private final String EXTERNAL_FUNCTION_ID = "/not-my-application/function/route";
    private final String FUNCTION_ID = "function/my_func";

    @Mock
    private HttpClient httpClient;
    @Captor
    private ArgumentCaptor<HttpClient.HttpRequest> requestCaptor;

    @Before
    public void setUp() {
        client = new CloudCompleterApiClient("http://localhost", httpClient);
    }


    @Test public void runtimeShouldParseACloudThreadId() throws IOException {
        String createCloudThreadPath = "/graph?functionId=" + FUNCTION_ID;
        when(httpClient.execute(any(HttpClient.HttpRequest.class))).thenReturn(
                new HttpClient.HttpResponse(200).addHeader(THREAD_ID_HEADER, THREAD_ID)
        );

        ThreadId tid = client.createThread(FUNCTION_ID);
        verify(httpClient, times(1)).execute(requestCaptor.capture());
        HttpClient.HttpRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.url).contains(createCloudThreadPath);
        assertThat(capturedRequest.method).isEqualTo("POST");
        assertThat(tid.getId()).isEqualTo(THREAD_ID);
    }

    @Test public void supplyStageCreation() throws IOException {
        String stageCreationPath = "/graph/" + THREAD_ID + "/supply";
        CloudThreads.SerSupplier<Integer> supplier = () -> 1;
        byte[] serializedBody = TestSerUtils.serializeToBytes(supplier);
        when(httpClient.execute(any(HttpClient.HttpRequest.class))).thenReturn(
                new HttpClient.HttpResponse(200)
                        .addHeader(STAGE_ID_HEADER, "1")
        );


        CompletionId cid = client.supply(new ThreadId(THREAD_ID), supplier);
        verify(httpClient, times(1)).execute(requestCaptor.capture());
        HttpClient.HttpRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.url).contains(stageCreationPath);
        assertThat(capturedRequest.method).isEqualTo("POST");
        assertThat(capturedRequest.headers)
                .containsEntry(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB)
                .containsEntry(CONTENT_TYPE_HEADER, CONTENT_TYPE_JAVA_OBJECT);
        assertThat(capturedRequest.bodyBytes).isEqualTo(serializedBody);
        assertThat(cid.getId()).isEqualTo("1");
    }

    @Test
    public void invokeFunctionReturnsStageId() throws IOException {
        String stageCreationPath = "/graph/" + THREAD_ID + "/invokeFunction";
        byte[] inputToFunction = "{ \"field\": \"contents\" }".getBytes();
        HashMap<String, String> headersCollection = new HashMap<>();
        headersCollection.put("My-Custom-Header", "Value");
        headersCollection.put("Content-Type", "application/json");
        Headers headers = Headers.fromMap(headersCollection);
        when(httpClient.execute(any(HttpClient.HttpRequest.class))).thenReturn(
                new HttpClient.HttpResponse(200)
                        .addHeader(STAGE_ID_HEADER, "1")
        );

        CompletionId cid = client.invokeFunction(new ThreadId(THREAD_ID), EXTERNAL_FUNCTION_ID, inputToFunction, HttpMethod.POST, headers);
        verify(httpClient, times(1)).execute(requestCaptor.capture());
        HttpClient.HttpRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.url).contains(stageCreationPath);
        assertThat(capturedRequest.query).containsEntry("functionId", EXTERNAL_FUNCTION_ID);
        assertThat(capturedRequest.headers)
                .containsEntry(DATUM_TYPE_HEADER, DATUM_TYPE_HTTP_REQ)
                .containsEntry(METHOD_HEADER, "POST")
                .containsEntry(USER_HEADER_PREFIX + "My-Custom-Header", "Value")
                .containsEntry(CONTENT_TYPE_HEADER, "application/json");
        assertThat(capturedRequest.bodyBytes).isEqualTo(inputToFunction);
        assertThat(cid.getId()).isEqualTo("1");
    }


}
