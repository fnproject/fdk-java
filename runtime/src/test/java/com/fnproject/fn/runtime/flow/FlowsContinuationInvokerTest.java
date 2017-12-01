package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.*;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.QueryParametersImpl;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

import static com.fnproject.fn.runtime.flow.FlowContinuationInvoker.FLOW_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

public class FlowsContinuationInvokerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final String FLOW_ID = "flow";
    private final String STAGE_ID = "stage_id";

    private TestBlobStore blobStore = new TestBlobStore();
    private FlowContinuationInvoker invoker = new FlowContinuationInvoker();

    @Before
    public void setupClient() {
        FlowRuntimeGlobals.setCompleterClientFactory(new CompleterClientFactory() {
            @Override
            public CompleterClient getCompleterClient() {
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public BlobStoreClient getBlobStoreClient() {
                return blobStore;
            }
        });
    }

    @After
    public void tearDownClient() {
        FlowRuntimeGlobals.resetCompleterClientFactory();
    }


    @Test
    public void continuationInvokedWhenGraphHeaderPresent() throws IOException, ClassNotFoundException {

        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerFunction<Integer, Integer>) (x) -> x * 2)
           .withJavaObjectArgs(10)
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();
        assertThat(blobStore.deserializeBlobResult(response.result, Integer.class)).isEqualTo(20);

    }


    @Test
    public void continuationNotInvokedWhenHeaderMissing() throws IOException, ClassNotFoundException {

        // Given

        InputEvent event = new InputEventBuilder()
           .withBody("")
           .build();

        // When
        FlowContinuationInvoker invoker = new FlowContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isNotPresent();
    }


    @Test
    public void failsIfArgMissing() throws IOException, ClassNotFoundException {
        thrown.expect(FunctionInputHandlingException.class);

        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerFunction<Integer, Integer>) (x) -> x * 2)
           .asEvent();

        invoker.tryInvoke(new EmptyInvocationContext(), event);


    }

    private static class TestIf implements Serializable {
        void call() {

        }
    }

    @Test
    public void failsIfUnknownClosureType() {
        thrown.expect(FunctionInputHandlingException.class);
        // Given
        InputEvent event = newRequest()
           .withClosure(new TestIf())
           .asEvent();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }


    @Test
    public void handlesAllClosureTypes() {
        class Tc {
            private final Serializable closure;
            private final Object args[];
            private final Object result;

            private Tc(Serializable closure, Object result, Object... args) {
                this.closure = closure;

                this.result = result;
                this.args = args;
            }
        }

        Tc[] cases = new Tc[]{
           new Tc((Flows.SerConsumer<String>) (v) -> {
           }, null, "hello"),
           new Tc((Flows.SerBiFunction<String, String, String>) (String::concat), "hello bob", "hello ", "bob"),
           new Tc((Flows.SerBiConsumer<String, String>) (a, b) -> {
           }, null, "hello ", "bob"),
           new Tc((Flows.SerFunction<String, String>) (String::toUpperCase), "HELLO BOB", "hello bob"),
           new Tc((Flows.SerRunnable) () -> {
           }, null),
           new Tc((Flows.SerCallable<String>) () -> "hello", "hello"),
           new Tc((Flows.SerSupplier<String>) () -> "hello", "hello"),

        };


        for (Tc tc : cases) {
            InputEvent event = newRequest()
               .withClosure(tc.closure)
               .withJavaObjectArgs(tc.args)
               .asEvent();
            Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

            assertThat(result).isPresent();
            APIModel.InvokeStageResponse res = fromOutput(result.get());

            if (tc.result == null) {
                assertThat(res.result.result).isInstanceOf(APIModel.EmptyDatum.class);
            } else {
                assertThat(blobStore.deserializeBlobResult(res.result, Object.class)).isEqualTo(tc.result);
            }

        }
    }

    @Test
    public void emptyValueCorrectlySerialized() throws IOException, ClassNotFoundException {
        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerConsumer<Object>) (x) -> {
               if (x != null) {
                   throw new RuntimeException("Not Null");
               }
           })
           .withEmptyDatumArg()
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();

    }

    @Test
    public void emptyValueCorrectlyDeSerialized() throws IOException, ClassNotFoundException {
        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerSupplier<Object>) () -> null)
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();
        assertThat(response.result.result).isInstanceOf(APIModel.EmptyDatum.class);

    }

    @Test
    public void stageRefCorrectlyDeserialized() throws IOException, ClassNotFoundException {

        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerConsumer<FlowFuture<Object>>) (x) -> {
               assertThat(x).isNotNull();
               assertThat(((RemoteFlow.RemoteFlowFuture<Object>) x).id()).isEqualTo("newStage");
           })
           .withStageRefArg("newStage")
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();

    }

    @Test
    public void stageRefCorrectlySerialized() throws IOException, ClassNotFoundException {
        RemoteFlow rf = new RemoteFlow(new FlowId(FLOW_ID));
        FlowFuture<Object> ff = rf.createFlowFuture(new CompletionId("newStage"));

        // Given
        InputEvent event = newRequest()
           .withClosure((Flows.SerSupplier<FlowFuture<Object>>) () -> ff)
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();
        assertThat(response.result.result).isInstanceOf(APIModel.StageRefDatum.class);

        assertThat(((APIModel.StageRefDatum) response.result.result).stageId).isEqualTo("newStage");

    }


    @Test
    public void setsCurrentStageId() throws IOException, ClassNotFoundException {

        InputEvent event = newRequest()
           .withClosure((Flows.SerRunnable) () -> {
               assertThat(FlowRuntimeGlobals.getCurrentCompletionId()).contains(new CompletionId("myStage"));
           })
           .withStageId("myStage")
           .asEvent();

        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse response = fromOutput(result.get());
        assertThat(response.result.successful).isTrue();

    }

    @Test
    public void httpRespToFn() throws Exception {
        // Given

        InputEvent event = newRequest()
           .withClosure((Flows.SerConsumer<HttpResponse>) (x) -> {
               assertThat(x.getBodyAsBytes()).isEqualTo("Hello".getBytes());
               assertThat(x.getStatusCode()).isEqualTo(201);
               assertThat(x.getHeaders().get("Foo")).contains("Bar");
           })
           .withHttpRespArg(201, "Hello", APIModel.HTTPHeader.create("Foo", "Bar"))
           .asEvent();


        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse resp = fromOutput(result.get());
        assertThat(resp.result.successful).isTrue();
    }


    @Test
    public void httpRespToFnWithError() throws Exception {
        // Given

        InputEvent event = newRequest()
           .withClosure((Flows.SerConsumer<FunctionInvocationException>) (e) -> {
               HttpResponse x = e.getFunctionResponse();
               assertThat(x.getBodyAsBytes()).isEqualTo("Hello".getBytes());
               assertThat(x.getStatusCode()).isEqualTo(201);
               assertThat(x.getHeaders().get("Foo")).contains("Bar");
           })
           .withHttpRespArg(false, 201, "Hello", APIModel.HTTPHeader.create("Foo", "Bar"))
           .asEvent();


        // When
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertThat(result).isPresent();
        APIModel.InvokeStageResponse resp = fromOutput(result.get());
        assertThat(resp.result.successful).isTrue();
    }

    @Test
    public void platformErrorsBecomeExceptions() throws IOException, ClassNotFoundException {

        class TestCase {
            private final APIModel.ErrorType errorType;
            private final Class<? extends Throwable> exceptionType;

            TestCase(APIModel.ErrorType errorType, Class<? extends Throwable> exceptionType) {
                this.errorType = errorType;
                this.exceptionType = exceptionType;
            }
        }
        for (TestCase tc : new TestCase[]{
           new TestCase(APIModel.ErrorType.InvalidStageResponse, InvalidStageResponseException.class),
           new TestCase(APIModel.ErrorType.FunctionInvokeFailed, FunctionInvokeFailedException.class),
           new TestCase(APIModel.ErrorType.FunctionTimeout, FunctionTimeoutException.class),
           new TestCase(APIModel.ErrorType.StageFailed, StageInvokeFailedException.class),
           new TestCase(APIModel.ErrorType.StageTimeout, StageTimeoutException.class),
           new TestCase(APIModel.ErrorType.StageLost, StageLostException.class)

        }) {
            Class<? extends Throwable> type = tc.exceptionType;
            // Given
            InputEvent event = newRequest()
               .withClosure((Flows.SerConsumer<Throwable>) (e) -> {

                   assertThat(e).hasMessage("My Error");
                   assertThat(e).isInstanceOf(type);
               })
               .withErrorBody(tc.errorType, "My Error")
               .asEvent();


            // When
            Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

            // Then
            assertThat(result).isPresent();
            APIModel.InvokeStageResponse resp = fromOutput(result.get());
            assertThat(resp.result.successful).isTrue();
        }
    }


    @Test
    public void functionInvocationExceptionThrownIfStageResultIsNotSerializable() {
        thrown.expect(ResultSerializationException.class);
        InputEvent event = newRequest()
           .withClosure((Flows.SerSupplier<Object>) Object::new)
           .asEvent();

        invoker.tryInvoke(new EmptyInvocationContext(), event);

    }


    private static class MyRuntimeException extends RuntimeException {
    }

    @Test
    public void functionInvocationExceptionThrownIfStageThrowsException() {
        thrown.expect(InternalFunctionInvocationException.class);
        thrown.expectCause(instanceOf(RuntimeException.class));
        thrown.expectMessage("Error invoking flows lambda");


        InputEvent event = newRequest()
           .withClosure((Flows.SerRunnable) () -> {
               throw new MyRuntimeException();
           })
           .asEvent();

        invoker.tryInvoke(new EmptyInvocationContext(), event);

    }


    public static class FlowRequestBuilder {
        private String flowId;

        private final TestBlobStore blobStore;

        APIModel.InvokeStageRequest req = new APIModel.InvokeStageRequest();

        public FlowRequestBuilder(TestBlobStore blobStore, String flowId, String stageId) {
            this.flowId = flowId;
            req.flowId = flowId;
            req.stageId = stageId;
            this.blobStore = blobStore;
        }


        public FlowRequestBuilder withStageId(String stageId) {
            req.stageId = stageId;
            return this;
        }

        public FlowRequestBuilder withClosure(Serializable closure) {
            req.closure = blobStore.withJavaBlob(flowId, closure);
            return this;
        }


        public FlowRequestBuilder withJavaObjectArgs(Object... args) {
            Arrays.stream(args).forEach((arg) ->
               req.args.add(blobStore.withResult(flowId, arg, true)));
            return this;
        }

        public InputEvent asEvent() {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] body;
            try {
                body = objectMapper.writeValueAsBytes(req);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            System.err.println("Req:" + new String(body));

            return new InputEventBuilder()
               .withHeader(FLOW_ID_HEADER, flowId)
               .withHeader("Content-type", "application/json")
               .withBody(new ByteArrayInputStream(body))
               .build();
        }

        public FlowRequestBuilder withEmptyDatumArg() {
            APIModel.CompletionResult res = new APIModel.CompletionResult();
            res.successful = true;
            res.result = new APIModel.EmptyDatum();
            req.args.add(res);
            return this;
        }

        public FlowRequestBuilder withStageRefArg(String stage) {
            APIModel.StageRefDatum stageRefDatum = new APIModel.StageRefDatum();
            stageRefDatum.stageId = stage;

            APIModel.CompletionResult res = new APIModel.CompletionResult();
            res.result = stageRefDatum;
            res.successful = true;
            req.args.add(res);

            return this;
        }

        public FlowRequestBuilder withHttpRespArg(boolean status, int code, String body, APIModel.HTTPHeader... httpHeaders) {
            APIModel.HTTPResp resp = new APIModel.HTTPResp();
            resp.statusCode = code;
            BlobResponse blobResponse = blobStore.writeBlob(flowId, body.getBytes(), "application/octet");

            resp.body = APIModel.Blob.fromBlobResponse(blobResponse);
            resp.headers = Arrays.asList(httpHeaders);
            APIModel.CompletionResult res = new APIModel.CompletionResult();
            APIModel.HTTPRespDatum datum = new APIModel.HTTPRespDatum();
            datum.resp = resp;
            res.result = datum;
            res.successful = status;
            req.args.add(res);
            return this;
        }

        public FlowRequestBuilder withHttpRespArg(int code, String body, APIModel.HTTPHeader... httpHeaders) {
            return withHttpRespArg(true, code, body, httpHeaders);

        }

        public FlowRequestBuilder withErrorBody(APIModel.ErrorType errType, String message) {
            APIModel.ErrorDatum errorDatum = new APIModel.ErrorDatum();
            errorDatum.type = errType;
            errorDatum.message = message;
            APIModel.CompletionResult result = new APIModel.CompletionResult();
            result.successful = false;
            result.result = errorDatum;
            req.args.add(result);
            return this;
        }
    }

    FlowRequestBuilder newRequest() {
        return new FlowRequestBuilder(blobStore, FLOW_ID, STAGE_ID);
    }

    private APIModel.InvokeStageResponse fromOutput(OutputEvent result) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            result.writeToOutput(bos);

            System.err.println("Result: " + new String(bos.toByteArray()));
            return new ObjectMapper().readValue(bos.toByteArray(), APIModel.InvokeStageResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class InputEventBuilder {

        private InputStream body;
        private Headers headers = Headers.fromMap(Collections.emptyMap());

        InputEventBuilder() {
        }

        public InputEventBuilder withBody(InputStream body) {
            this.body = body;
            return this;
        }


        public InputEventBuilder withBody(String body) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                bos.write(body.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.body = new ByteArrayInputStream(bos.toByteArray());
            return this;
        }

        private Map<String, String> currentHeaders() {
            return new HashMap<>(headers.getAll());
        }

        public InputEventBuilder withHeaders(Map<String, String> headers) {
            Map<String, String> updated = currentHeaders();
            updated.putAll(headers);
            this.headers = Headers.fromMap(updated);
            return this;
        }

        public InputEventBuilder withHeader(String name, String value) {
            Map<String, String> updated = currentHeaders();
            updated.put(name, value);
            this.headers = Headers.fromMap(updated);
            return this;
        }

        private String getHeader(String key) {
            for (Map.Entry<String, String> entry : currentHeaders().entrySet()) {
                if (key.equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public InputEvent build() {
            return new ReadOnceInputEvent(
               "", "", "", "",
               body,
               headers, new QueryParametersImpl());
        }
    }

    class EmptyRuntimeContext implements RuntimeContext {

        @Override
        public Optional<Object> getInvokeInstance() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getConfigurationByKey(String key) {
            return Optional.empty();
        }

        @Override
        public Map<String, String> getConfiguration() {
            return null;
        }

        @Override
        public <T> Optional<T> getAttribute(String att, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void setAttribute(String att, Object val) {
            throw new RuntimeException("You can't modify the empty runtime context in the tests, sorry.");
        }

        @Override
        public void addInputCoercion(InputCoercion ic) {
            throw new RuntimeException("You can't modify the empty runtime context in the tests, sorry.");
        }

        @Override
        public List<InputCoercion> getInputCoercions(MethodWrapper targetMethod, int param) {
            return null;
        }

        @Override
        public void addOutputCoercion(OutputCoercion oc) {
            throw new RuntimeException("You can't modify the empty runtime context in the tests, sorry.");
        }

        @Override
        public List<OutputCoercion> getOutputCoercions(Method method) {
            return null;
        }

        @Override
        public void setInvoker(FunctionInvoker invoker) {
            throw new RuntimeException("You can't modify the empty runtime context in the tests, sorry.");
        }

        @Override
        public MethodWrapper getMethod() {
            return null;
        }

    }

    class EmptyInvocationContext implements InvocationContext {

        @Override
        public RuntimeContext getRuntimeContext() {
            return new EmptyRuntimeContext();
        }

        @Override
        public void addListener(InvocationListener listener) {
        }

    }
}
