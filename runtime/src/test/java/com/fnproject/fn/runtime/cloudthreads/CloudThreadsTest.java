package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.cloudthreads.CloudThreads;
import com.fnproject.fn.runtime.FnTestHarness;
import com.fnproject.fn.runtime.TestSerUtils;
import com.fnproject.fn.runtime.testfns.CloudThreadsFn;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class CloudThreadsTest {

    @Rule
    public FnTestHarness fnTestHarness = new FnTestHarness();

    private final String FUNCTION_ID = "app/testfn";
    private final ThreadId THREAD_ID = new ThreadId("test-thread-id");

    // static to avoid issues with serialized AtomicRefs
    static AtomicBoolean tag = new AtomicBoolean(false);

    @Mock
    CompleterClient mockCompleterClient;

    @Before
    public void setup() {
        tag.set(false);
        MockitoAnnotations.initMocks(this);
        CloudThreadsContinuationInvoker.resetCompleterClientFactory();

        CloudThreadsContinuationInvoker.setCompleterClientFactory(() -> mockCompleterClient);
    }

    private FnTestHarness.EventBuilder eventToTestFN() {
        return fnTestHarness.givenDefaultEvent().withAppName("app").withRoute("/testfn");
    }

    private FnTestHarness.EventBuilder httpEventToTestFN() {
        return fnTestHarness.givenHttpEvent()
                .withAppName("app")
                .withRoute("/testfn");
    }

    @Test
    public void completerNotCalledIfCloudThreadsRuntimeUnused() throws Exception {

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(CloudThreadsFn.class, "notUsingCloudThreads");

        verify(mockCompleterClient, never()).createThread(any());
    }

    @Test
    public void completerCalledWhenCloudThreadsRuntimeIsAccessed() {

        when(mockCompleterClient.createThread(FUNCTION_ID)).thenReturn(THREAD_ID);

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(CloudThreadsFn.class, "usingCloudThreads");

        verify(mockCompleterClient, times(1)).createThread(FUNCTION_ID);
    }

    @Test
    public void onlyOneThreadIsCreatedWhenRuntimeIsAccessedMultipleTimes() {

        when(mockCompleterClient.createThread(FUNCTION_ID)).thenReturn(THREAD_ID);

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(CloudThreadsFn.class, "accessRuntimeMultipleTimes");

        verify(mockCompleterClient, times(1)).createThread(FUNCTION_ID);
    }

    @Test
    public void invokeWithinAsyncFunction() throws InterruptedException, IOException, ClassNotFoundException {

        AtomicReference<Object> continuationResult = new AtomicReference<>();
        CompletionId completionId = new CompletionId("continuation-completion-id");

        when(mockCompleterClient.createThread(FUNCTION_ID)).thenReturn(THREAD_ID);

        when(mockCompleterClient.supply(eq(THREAD_ID),
                isA(CloudThreads.SerCallable.class)))
                .thenAnswer(invokeContinuation(completionId, continuationResult, "supplyAndGetResult"));
        when(mockCompleterClient.waitForCompletion(eq(THREAD_ID), eq(completionId)))
                .thenAnswer(invocationOnMock -> continuationResult.get());

        httpEventToTestFN().enqueue();
        fnTestHarness.thenRun(CloudThreadsFn.class, "supplyAndGetResult");

        FnTestHarness.ParsedHttpResponse response = getSingleItem(fnTestHarness.getParsedHttpResponses());
        assertThat(response.getBodyAsString()).isEqualTo(continuationResult.toString());
        verify(mockCompleterClient, times(1))
                .supply(eq(THREAD_ID), isA(CloudThreads.SerCallable.class));
        verify(mockCompleterClient, times(1))
                .waitForCompletion(eq(THREAD_ID), eq(completionId));
    }


    /**
     * Mock the behaviour of a call to the Completer service through supply
     * <p>
     * When called by Mockito in response to a matching method call,
     * starts a function using the test harness, puts the result into a shared
     * AtomicReference, and returns the supplied Completion Id.
     *
     * @param completionId CompletionId to return from the invocation
     * @param result       The result from invoking the continuation
     * @param methodName
     * @return a Mockito Answer instance providing the mock behaviour
     */
    private Answer<CompletionId> invokeContinuation(CompletionId completionId, AtomicReference<Object> result, String methodName) {
        return fn -> {
            if (fn.getArguments().length == 2) {

                CloudThreads.SerCallable closure = fn.getArgument(1);

                TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
                        .addJavaEntity(closure);

                FnTestHarness fnTestHarness = new FnTestHarness();
                fnTestHarness.givenHttpEvent().withAppName("app").withRoute("/testfn")
                        .withBody(ser.getContentStream())
                        .withHeader(THREAD_ID_HEADER, THREAD_ID.getId())
                        .withHeaders(ser.getHeaders())
                        .enqueue();

                fnTestHarness.thenRun(CloudThreadsFn.class, methodName);

                FnTestHarness.ParsedHttpResponse response = getInnerResponse(fnTestHarness);
                try {
                    assertThat(normalisedHeaders(response))
                            .containsEntry(DATUM_TYPE_HEADER.toLowerCase(), DATUM_TYPE_BLOB)
                            .containsEntry(CONTENT_TYPE_HEADER.toLowerCase(), CONTENT_TYPE_JAVA_OBJECT);
                    result.set(SerUtils.deserializeObject(response.getBodyAsBytes()));
                } catch (Exception e) {
                    result.set(e);
                }

                return completionId;
            } else {
                throw new RuntimeException("Too few arguments given to supply");
            }
        };
    }




    @Test
    public void capturedCallableIsInvoked() throws Exception {

        Callable<String> r = (CloudThreads.SerCallable<String>) () -> "Foo Bar";

        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
                .addJavaEntity(r);

        httpEventToTestFN()
                .withHeader(THREAD_ID_HEADER, THREAD_ID.getId())
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .enqueue();

        fnTestHarness.thenRun(CloudThreadsFn.class, "supply");

        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo("Foo Bar");
    }

    @Test
    public void capturedRunnableIsInvoked() throws Exception {
        Runnable r = (CloudThreads.SerRunnable) () -> {
            tag.set(true);
        };

        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
                .addJavaEntity(r);

        httpEventToTestFN()
                .withHeader(THREAD_ID_HEADER, THREAD_ID.getId())
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .enqueue();

        fnTestHarness.thenRun(CloudThreadsFn.class, "supply");
        assertThat(tag.get()).isTrue();
    }


    @Test
    public void capturedFunctionWithArgsIsInvoked() throws Exception {
        Function<String,String> func = (CloudThreads.SerFunction<String,String>) (in) ->"Foo" + in;

        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
                .addJavaEntity(func)
                .addJavaEntity("BAR");

        httpEventToTestFN()
                .withHeader(THREAD_ID_HEADER, THREAD_ID.getId())
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .enqueue();

        fnTestHarness.thenRun(CloudThreadsFn.class, "supply");

        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo("FooBAR");
    }

    private Object getResultObjectFromSingleResponse(FnTestHarness fnTestHarness) throws IOException, ClassNotFoundException {
        FnTestHarness.ParsedHttpResponse innerResponse = getInnerResponse(fnTestHarness);
        assertThat(normalisedHeaders(innerResponse))
                .containsEntry(DATUM_TYPE_HEADER.toLowerCase(), DATUM_TYPE_BLOB)
                .containsEntry(CONTENT_TYPE_HEADER.toLowerCase(), CONTENT_TYPE_JAVA_OBJECT);
        return SerUtils.deserializeObject(innerResponse.getBodyAsBytes());
    }

    private FnTestHarness.ParsedHttpResponse getInnerResponse(FnTestHarness fnTestHarness) {
        FnTestHarness.ParsedHttpResponse response = getSingleItem(fnTestHarness.getParsedHttpResponses());
        return getSingleItem(FnTestHarness.getParsedHttpResponses(response.getBodyAsBytes()));
    }

    private <T> T getSingleItem(List<T> items) {
        assertThat(items.size()).isEqualTo(1);
        return items.get(0);
    }

    private Map<String, String> normalisedHeaders(FnTestHarness.ParsedHttpResponse response) {
        return response.getHeaders().entrySet().stream()
                .collect(Collectors.toMap((kv) -> kv.getKey().toLowerCase(), Map.Entry::getValue));
    }

    @Test
    public void capturedRunnableCanGetCurrentCloudThreadRuntime() throws Exception {
        Callable<String> r = (CloudThreads.SerCallable<String>) () -> {
            return CloudThreads.currentRuntime().getClass().getName();
        };

        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
                .addJavaEntity(r);

        httpEventToTestFN()
                .withHeader(THREAD_ID_HEADER, THREAD_ID.getId())
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .enqueue();

        fnTestHarness.thenRun(CloudThreadsFn.class, "supply");

        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo(RemoteCloudThreadRuntime.class.getName());
    }

    //NotSerializedResult
    //Throws Exception in closure
    //throws unserialized exception in closure
    //null result from closure
    //null value to closure.
}
