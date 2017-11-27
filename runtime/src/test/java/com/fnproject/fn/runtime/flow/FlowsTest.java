package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.runtime.FnTestHarness;
import com.fnproject.fn.runtime.testfns.FnFlowsFunction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

public class FlowsTest {

    @Rule
    public FnTestHarness fnTestHarness = new FnTestHarness();

    private final String FUNCTION_ID = "app/testfn";
    private final FlowId FLOW_ID = new FlowId("test-flow-id");

    // static to avoid issues with serialized AtomicRefs
    static AtomicBoolean tag = new AtomicBoolean(false);

    @Mock
    CompleterClient mockCompleterClient;

    TestBlobStore testBlobStore;

    @Before
    public void setup() {
        tag.set(false);
        MockitoAnnotations.initMocks(this);
        FlowRuntimeGlobals.resetCompleterClientFactory();

        FlowRuntimeGlobals.setCompleterClientFactory(new CompleterClientFactory() {
            @Override
            public CompleterClient getCompleterClient() {
                return mockCompleterClient;
            }

            @Override
            public BlobStoreClient getBlobStoreClient() {
                return testBlobStore;
            }
        });
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
    public void completerNotCalledIfFlowRuntimeUnused() throws Exception {

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(FnFlowsFunction.class, "notUsingFlows");

        verify(mockCompleterClient, never()).createFlow(any());
    }

    @Test
    public void completerCalledWhenFlowRuntimeIsAccessed() {

        when(mockCompleterClient.createFlow(FUNCTION_ID)).thenReturn(FLOW_ID);

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(FnFlowsFunction.class, "usingFlows");

        verify(mockCompleterClient, times(1)).createFlow(FUNCTION_ID);
    }

    @Test
    public void onlyOneThreadIsCreatedWhenRuntimeIsAccessedMultipleTimes() {

        when(mockCompleterClient.createFlow(FUNCTION_ID)).thenReturn(FLOW_ID);

        eventToTestFN().enqueue();
        fnTestHarness.thenRun(FnFlowsFunction.class, "accessRuntimeMultipleTimes");

        verify(mockCompleterClient, times(1)).createFlow(FUNCTION_ID);
    }

//    @Test
//    public void invokeWithinAsyncFunction() throws InterruptedException, IOException, ClassNotFoundException {
//
//        AtomicReference<Object> continuationResult = new AtomicReference<>();
//        CompletionId completionId = new CompletionId("continuation-completion-id");
//
//        when(mockCompleterClient.createFlow(FUNCTION_ID)).thenReturn(FLOW_ID);
//
//        when(mockCompleterClient.supply(eq(FLOW_ID),
//                isA(Flows.SerCallable.class),isA(CodeLocation.class)))
//                .thenAnswer(invokeContinuation(completionId, continuationResult, "supplyAndGetResult"));
//        when(mockCompleterClient.waitForCompletion(eq(FLOW_ID), eq(completionId), eq(getClass().getClassLoader())))
//                .thenAnswer(invocationOnMock -> continuationResult.get());
//
//        httpEventToTestFN().enqueue();
//        fnTestHarness.thenRun(FnFlowsFunction.class, "supplyAndGetResult");
//
//        FnTestHarness.ParsedHttpResponse response = getSingleItem(fnTestHarness.getParsedHttpResponses());
//        assertThat(response.getBodyAsString()).isEqualTo(continuationResult.toString());
//        ArgumentCaptor<CodeLocation> locCaptor = ArgumentCaptor.forClass(CodeLocation.class);
//        verify(mockCompleterClient, times(1))
//                .supply(eq(FLOW_ID), isA(Flows.SerCallable.class), locCaptor.capture());
//
//        CodeLocation gotLocation = locCaptor.getValue();
//        assertThat(gotLocation.getLocation())
//                .matches(Pattern.compile("com\\.fnproject\\.fn\\.runtime\\.testfns\\.FnFlowsFunction\\.supplyAndGetResult\\(.*\\.java\\:\\d+\\)"));
//        verify(mockCompleterClient, times(1))
//                .waitForCompletion(eq(FLOW_ID), eq(completionId), eq(getClass().getClassLoader()));
//    }
//
//    /**
//     * Mock the behaviour of a call to the Completer service through supply
//     * <p>
//     * When called by Mockito in response to a matching method call,
//     * starts a function using the test harness, puts the result into a shared
//     * AtomicReference, and returns the supplied Completion Id.
//     *
//     * @param completionId CompletionId to return from the invocation
//     * @param result       The result from invoking the continuation
//     * @param methodName
//     * @return a Mockito Answer instance providing the mock behaviour
//     */
//    private Answer<CompletionId> invokeContinuation(CompletionId completionId, AtomicReference<Object> result, String methodName) {
//        return fn -> {
//            if (fn.getArguments().length == 3) {
//
//                Flows.SerCallable closure = fn.getArgument(1);
//
//
//                FnTestHarness fnTestHarness = new FnTestHarness();
//              `
//                fnTestHarness.thenRun(FnFlowsFunction.class, methodName);
//
//                FnTestHarness.ParsedHttpResponse response = getInnerResponse(fnTestHarness);
//                try {
//                    assertThat(normalisedHeaders(response))
//                            .containsEntry(DATUM_TYPE_HEADER.toLowerCase(), DATUM_TYPE_BLOB)
//                            .containsEntry(CONTENT_TYPE_HEADER.toLowerCase(), CONTENT_TYPE_JAVA_OBJECT);
//                    result.set(SerUtils.deserializeObject(response.getBodyAsBytes()));
//                } catch (Exception e) {
//                    result.set(e);
//                }
//
//                return completionId;
//            } else {
//                throw new RuntimeException("Too few arguments given to supply");
//            }
//        };
//    }
//
//
//
//
//    @Test
//    public void capturedCallableIsInvoked() throws Exception {
//
//        Callable<String> r = (Flows.SerCallable<String>) () -> "Foo Bar";
//
//        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
//                .addJavaEntity(r);
//
//        httpEventToTestFN()
//                .withHeader(FLOW_ID_HEADER, FLOW_ID.getId())
//                .withHeaders(ser.getHeaders())
//                .withBody(ser.getContentStream())
//                .enqueue();
//
//        fnTestHarness.thenRun(FnFlowsFunction.class, "supply");
//
//        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo("Foo Bar");
//    }
//
//    @Test
//    public void capturedRunnableIsInvoked() throws Exception {
//        Runnable r = (Flows.SerRunnable) () -> {
//            tag.set(true);
//        };
//
//        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
//                .addJavaEntity(r);
//
//        httpEventToTestFN()
//                .withHeader(FLOW_ID_HEADER, FLOW_ID.getId())
//                .withHeaders(ser.getHeaders())
//                .withBody(ser.getContentStream())
//                .enqueue();
//
//        fnTestHarness.thenRun(FnFlowsFunction.class, "supply");
//        assertThat(tag.get()).isTrue();
//    }
//
//
//    @Test
//    public void capturedFunctionWithArgsIsInvoked() throws Exception {
//        Function<String,String> func = (Flows.SerFunction<String,String>) (in) ->"Foo" + in;
//
//        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
//                .addJavaEntity(func)
//                .addJavaEntity("BAR");
//
//        httpEventToTestFN()
//                .withHeader(FLOW_ID_HEADER, FLOW_ID.getId())
//                .withHeaders(ser.getHeaders())
//                .withBody(ser.getContentStream())
//                .enqueue();
//
//        fnTestHarness.thenRun(FnFlowsFunction.class, "supply");
//
//        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo("FooBAR");
//    }
//
//    @Test
//    public void catastrophicFailureStillResultsInGraphCommitted() throws Exception {
//        when(mockCompleterClient.createFlow(FUNCTION_ID)).thenReturn(FLOW_ID);
//
//        httpEventToTestFN().enqueue();
//        fnTestHarness.thenRun(FnFlowsFunction.class, "createFlowAndThenFail");
//
//        verify(mockCompleterClient, times(1)).commit(FLOW_ID);
//    }
//
//    private Object getResultObjectFromSingleResponse(FnTestHarness fnTestHarness) throws IOException, ClassNotFoundException {
//        FnTestHarness.ParsedHttpResponse innerResponse = getInnerResponse(fnTestHarness);
//        assertThat(normalisedHeaders(innerResponse))
//                .containsEntry(DATUM_TYPE_HEADER.toLowerCase(), DATUM_TYPE_BLOB)
//                .containsEntry(CONTENT_TYPE_HEADER.toLowerCase(), CONTENT_TYPE_JAVA_OBJECT);
//        return SerUtils.deserializeObject(innerResponse.getBodyAsBytes());
//    }
//
//    private FnTestHarness.ParsedHttpResponse getInnerResponse(FnTestHarness fnTestHarness) {
//        FnTestHarness.ParsedHttpResponse response = getSingleItem(fnTestHarness.getParsedHttpResponses());
//        return getSingleItem(FnTestHarness.getParsedHttpResponses(response.getBodyAsBytes()));
//    }
//
//    private <T> T getSingleItem(List<T> items) {
//        assertThat(items.size()).isEqualTo(1);
//        return items.get(0);
//    }
//
//    private Map<String, String> normalisedHeaders(FnTestHarness.ParsedHttpResponse response) {
//        return response.getHeaders().entrySet().stream()
//                .collect(Collectors.toMap((kv) -> kv.getKey().toLowerCase(), Map.Entry::getValue));
//    }
//
//    @Test
//    public void capturedRunnableCanGetCurrentFlowRuntime() throws Exception {
//        Callable<String> r = (Flows.SerCallable<String>) () -> {
//            return Flows.currentFlow().getClass().getName();
//        };
//
//        TestSerUtils.HttpMultipartSerialization ser = new TestSerUtils.HttpMultipartSerialization()
//                .addJavaEntity(r);
//
//        httpEventToTestFN()
//                .withHeader(FLOW_ID_HEADER, FLOW_ID.getId())
//                .withHeaders(ser.getHeaders())
//                .withBody(ser.getContentStream())
//                .enqueue();
//
//        fnTestHarness.thenRun(FnFlowsFunction.class, "supply");
//
//        assertThat(getResultObjectFromSingleResponse(fnTestHarness)).isEqualTo(RemoteFlow.class.getName());
//    }

    //NotSerializedResult
    //Throws Exception in closure
    //throws unserialized exception in closure
    //null result from closure
    //null value to closure.
}
