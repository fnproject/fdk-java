package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.*;
import com.fnproject.fn.api.cloudthreads.*;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.runtime.QueryParametersImpl;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.function.DoubleSupplier;

import static com.fnproject.fn.runtime.TestSerUtils.*;
import static com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CloudThreadsContinuationInvokerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final String THREAD_ID = UUID.randomUUID().toString();
    private final String STAGE_ID = Integer.toString(new Random().nextInt(32));

    @Test
    public void continuationInvokedWhenGraphHeaderPresent() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerCallable<Integer>) () -> testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        Optional<Integer> resultValue = resultAsInteger(result.get());
        assertTrue(resultValue.isPresent());
        assertTrue(resultValue.equals(Optional.of(testValue)));
        assertSuccessfulResult(result.get());
    }

    @Test
    public void continuationNotInvokedWhenHeaderMissing() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerCallable<Integer>) () -> testValue);

        InputEvent event = new InputEventBuilder()
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .build();

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void successIfFunctionInvokedWithOneParam() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<Integer,Integer>) (x) -> x + testValue)
                .addJavaEntity(testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        Optional<Integer> resultValue = resultAsInteger(result.get());
        assertTrue(resultValue.isPresent());
        assertTrue(resultValue.equals(Optional.of(testValue + testValue)));
        assertSuccessfulResult(result.get());
    }

    @Test
    public void exceptionThrownIfFunctionInvokedWithoutParams() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<Integer,Integer>) (x) -> x + testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(FunctionInputHandlingException.class);
        thrown.expectMessage("Error reading continuation content");

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void successIfFunctionInvokedWithTwoParams() throws IOException, ClassNotFoundException {
        // XXX: do we really want this to succeed??

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<Integer,Integer>) (x) -> x + testValue)
                .addJavaEntity(testValue)
                .addJavaEntity(testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        Optional<Integer> resultValue = resultAsInteger(result.get());
        assertTrue(resultValue.isPresent());
        assertTrue(resultValue.equals(Optional.of(testValue + testValue)));
        assertSuccessfulResult(result.get());
    }

    @Test
    public void successIfSupplierInvokedWithParams() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerSupplier) () -> testValue)
                .addJavaEntity(testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        Optional<Integer> resultValue = resultAsInteger(result.get());
        assertTrue(resultValue.isPresent());
        assertTrue(resultValue.equals(Optional.of(testValue)));
        assertSuccessfulResult(result.get());
    }

    @Test
    public void exceptionThrownIfClosureDatumTypeMissing() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addEntity(CONTENT_TYPE_JAVA_OBJECT, serializeToBytes((CloudThreads.SerCallable<Integer>) () -> testValue), Collections.emptyMap());

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(FunctionInputHandlingException.class);
        thrown.expectMessage("Error deserializing closure object");

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void emptyValueCorrectlySerialized() throws IOException, ClassNotFoundException {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction) (x) -> x)
                .addEmptyEntity();

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        assertSucessfulEmptyResult(result.get());
    }

    @Test
    public void thenComposeStageCorrectlySerialized() throws Exception {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerCallable) () -> {
                            CompleterClient client = mock(CompleterClient.class, withSettings().serializable());
                            when(client.completedValue(any(), any())).thenReturn(new CompletionId("1"));
                            return new RemoteCloudThreadRuntime(new ThreadId("1"), (CompleterClientFactory) () -> client).completedValue(1);
                        }
                );

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        assertSuccessfulCompletionStageResult("1", result.get());
    }

    @Test
    public void externalFunctionInvocationPopulatesFunctionResponse() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("FnProject-Header-Custom-Header", "customValue");

        String functionResponse = "{ \"some\": \"json\" }";
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<HttpResponse, Boolean>) (result) -> {
                    // Expect
                    assertThat(result.getStatusCode()).isEqualTo(200);
                    assertThat(new String(result.getBodyAsBytes())).isEqualTo(functionResponse);
                    assertThat(result.getHeaders().get("Custom-Header"))
                            .isPresent()
                            .contains("customValue");
                    return true;
                })
                .addFnResultEntity(200, headers, "application/json", functionResponse);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void failedExternalFunctionInvocationDeserialisesToFunctionResponse() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("FnProject-Header-Custom-Header", "customValue");

        String functionResponse = "{ \"some\": \"json\" }";
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<HttpResponse, Boolean>) (result) -> {
                    // Then
                    assertThat(result.getStatusCode()).isEqualTo(500);
                    assertThat(new String(result.getBodyAsBytes())).isEqualTo(functionResponse);
                    assertThat(result.getHeaders().get("Custom-Header"))
                            .isPresent()
                            .contains("customValue");
                    return true;
                })
                .addFnResultEntity(500, headers, "application/json", functionResponse);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
    }


    @Test
    public void failedExternalFunctionInvocationCorrectlyCoercedToException() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("FnProject-Header-Custom-Header", "customValue");
        headers.put(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);


        String postedResult = "{ \"some\": \"json\" }";
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<Throwable, String>) (result) ->
                        new String(((FunctionInvocationException) result).getFunctionResponse().getBodyAsBytes()))
                .addFnResultEntity(500, headers, "application/json", postedResult);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
        assertThat(result).isPresent();
        assertThat(resultAsObject(result.get())).isEqualTo(postedResult);
    }

    @Test
    public void externallyCompletableResultPopulatesHttpRequest() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("FnProject-Header-Custom-Header", "customValue");

        String postedResult = "{ \"some\": \"json\" }";
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction<HttpRequest, Boolean>) (result) -> {
                    // Expect
                    assertThat(result.getMethod()).isEqualTo(HttpMethod.POST);
                    assertThat(new String(result.getBodyAsBytes())).isEqualTo(postedResult);
                    assertThat(result.getHeaders().get("Content-Type"))
                            .isPresent()
                            .contains("application/json");
                    assertThat(result.getHeaders().get("Custom-Header"))
                            .isPresent()
                            .contains("customValue");
                    return true;
                })
                .addExternalCompletionEntity("POST", headers, "application/json", postedResult);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void externallyCompletableResultFailureCorrectlyCoercedToException() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("FnProject-Header-Custom-Header", "customValue");
        headers.put(RESULT_STATUS_HEADER, RESULT_STATUS_FAILURE);

        String postedResult = "{ \"some\": \"json\" }";
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerBiFunction<Object, Throwable, String>) (result, error) ->
                                new String(((ExternalCompletionException) error).getExternalRequest().getBodyAsBytes()))
                .addEmptyEntity()
                .addExternalCompletionEntity("POST", headers, "application/json", postedResult);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);
        assertThat(result).isPresent();
        assertThat(resultAsObject(result.get())).isEqualTo(postedResult);
    }

    @Test
    public void deserializationSkipsExtraPadding() throws IOException, ClassNotFoundException {
        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addEntity(CONTENT_TYPE_JAVA_OBJECT,
                        concat(serializeToBytes((CloudThreads.SerFunction<Integer,Integer>) (x) -> x + testValue), new byte[1024]),
                        Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB))
                .addJavaEntity(testValue);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        Optional<Integer> resultValue = resultAsInteger(result.get());
        assertTrue(resultValue.isPresent());
        assertTrue(resultValue.equals(Optional.of(testValue + testValue)));
    }

    @Test
    public void exceptionThrownIfClosureContentTypeWrong() throws IOException, ClassNotFoundException {

        // Given
        final Integer testValue = 3;

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addEntity("application/octet-stream",
                        serializeToBytes((CloudThreads.SerCallable<Integer>) () -> testValue),
                        Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB));

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(FunctionInputHandlingException.class);
        thrown.expectMessage("Error deserializing closure object");

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void exceptionThrownIfBodyHasErrorType() throws IOException, ClassNotFoundException {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addErrorEntity(ERROR_TYPE_INVALID_STAGE_RESPONSE);

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(FunctionInputHandlingException.class);
        thrown.expectMessage("Error deserializing closure object");

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void platformErrorsBecomeExceptions() throws IOException, ClassNotFoundException {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerFunction) (x) -> x)
                .addErrorEntity(ERROR_TYPE_FUNCTION_TIMEOUT);

        InputEvent event = constructContinuationInputEvent(ser);

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        Optional<OutputEvent> result = invoker.tryInvoke(new EmptyInvocationContext(), event);

        // Then
        assertTrue(result.isPresent());
        assertThat(resultAsObject(result.get())).isInstanceOf(FunctionTimeoutException.class);
        assertSuccessfulResult(result.get());
    }


    @Test
    public void exceptionThrownIfDispatchPatternUnmatched() throws IOException {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((DoubleSupplier & Serializable) () -> 3);

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("no dispatch mechanism found for class");

        // When
        CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
        invoker.tryInvoke(new EmptyInvocationContext(), event);
    }

    @Test
    public void functionInvocationExceptionThrownIfStageResultIsNotSerializable() throws IOException, ClassNotFoundException {
        // Given
        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerSupplier) Object::new);

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(InternalFunctionInvocationException.class);
        thrown.expectCause(instanceOf(ResultSerializationException.class));
        thrown.expectMessage("Error handling response from cloud threads lambda");

        // When
        try {
            CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
            invoker.tryInvoke(new EmptyInvocationContext(), event);
        } catch(Exception ex) {
            assertThat(ex.getCause().getMessage()).isEqualTo("Result returned by stage is not serializable: java.lang.Object");
            throw ex;
        }
    }

    @Test
    public void functionInvocationExceptionThrownIfStageThrowsException() throws IOException, ClassNotFoundException {
        // Given
        final String exceptionMessage = "oh no";

        HttpMultipartSerialization ser = new HttpMultipartSerialization()
                .addJavaEntity((CloudThreads.SerSupplier) () -> {throw new RuntimeException(exceptionMessage);});

        InputEvent event = constructContinuationInputEvent(ser);

        // Then
        thrown.expect(InternalFunctionInvocationException.class);
        thrown.expectCause(instanceOf(RuntimeException.class));
        thrown.expectMessage("Error invoking cloud threads lambda");

        // When
        try {
            CloudThreadsContinuationInvoker invoker = new CloudThreadsContinuationInvoker();
            invoker.tryInvoke(new EmptyInvocationContext(), event);
        } catch(Exception ex) {
            assertThat(ex.getCause().getMessage()).isEqualTo(exceptionMessage);
            throw ex;
        }
    }

    private Object resultAsObject(OutputEvent result) throws IOException, ClassNotFoundException {
        return SerUtils.deserializeObject(((CloudThreadsContinuationInvoker.ContinuationOutputEvent)result).getContentBody());
    }

    private byte[] resultInnerPayload(OutputEvent result) throws IOException {
        return ((CloudThreadsContinuationInvoker.ContinuationOutputEvent)result).getContentBody();
    }

    private Optional<Integer> resultAsInteger(OutputEvent oe) {
        if (!(oe instanceof CloudThreadsContinuationInvoker.ContinuationOutputEvent)) {
            return Optional.empty();
        }

        CloudThreadsContinuationInvoker.ContinuationOutputEvent coe = (CloudThreadsContinuationInvoker.ContinuationOutputEvent) oe;

        // Check Datum Type
        if (!coe.getHeaders().get(DATUM_TYPE_HEADER).map((datumType) -> datumType.equalsIgnoreCase(DATUM_TYPE_BLOB)).isPresent()) {
            return Optional.empty();
        }

        // Check Content-Type
        if (!CONTENT_TYPE_JAVA_OBJECT.equalsIgnoreCase(coe.getInternalContentType())) {
            return Optional.empty();
        }

        // Deserialize body and reconstruct object
        try {
            return Optional.of((Integer) SerUtils.deserializeObject(coe.getContentBody()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertSucessfulEmptyResult(OutputEvent result) throws IOException {
        assertContinuationOutputEvent(result);
        assertThat(resultInnerPayload(result).length).isEqualTo(0);
        assertDatumType(DATUM_TYPE_EMPTY, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
        assertResultStatus(true, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
    }

    private void assertSuccessfulResult(OutputEvent result) {
        assertContinuationOutputEvent(result);
        assertContentType(CONTENT_TYPE_JAVA_OBJECT, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
        assertResultStatus(true, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
        assertDatumType(DATUM_TYPE_BLOB, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
    }

    private void assertSuccessfulCompletionStageResult(String expectedStageId, OutputEvent result) {
        assertContinuationOutputEvent(result);
        assertDatumType(DATUM_TYPE_STAGEREF, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
        assertResultStatus(true, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
        assertHeader(STAGE_ID_HEADER, expectedStageId, (CloudThreadsContinuationInvoker.ContinuationOutputEvent) result);
    }

    private void assertContinuationOutputEvent(OutputEvent result) {
        assertTrue(result instanceof CloudThreadsContinuationInvoker.ContinuationOutputEvent);
    }

    private static void assertContentType(String expectedContentType, CloudThreadsContinuationInvoker.ContinuationOutputEvent result) {
        String contentType = result.getInternalContentType();
        assertThat(contentType).isEqualTo(expectedContentType);
    }

    private void assertResultStatus(boolean status, CloudThreadsContinuationInvoker.ContinuationOutputEvent result) {
        assertThat(result.isSuccess()).isEqualTo(status);
    }

    private void assertDatumType(String datumType, CloudThreadsContinuationInvoker.ContinuationOutputEvent result) {
        assertHeader(DATUM_TYPE_HEADER, datumType, result);
    }

    private void assertHeader(String header, String value, CloudThreadsContinuationInvoker.ContinuationOutputEvent result) {
        assertThat(result.getHeaders().get(header))
                .contains(value);
    }

    private InputEvent constructContinuationInputEvent(HttpMultipartSerialization ser) throws IOException {
        return new InputEventBuilder()
                .withHeader(THREAD_ID_HEADER, THREAD_ID)
                .withHeader(STAGE_ID_HEADER, STAGE_ID)
                .withHeaders(ser.getHeaders())
                .withBody(ser.getContentStream())
                .build();
    }

    private class InputEventBuilder {

        private InputStream body;
        private Headers headers = Headers.fromMap(Collections.emptyMap());

        InputEventBuilder() {
        }

        public InputEventBuilder withBody(InputStream body) {
            this.body = body;
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
            for (Map.Entry<String, String> entry: currentHeaders().entrySet()) {
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
        public void addOutputCoercion(OutputCoercion oc) {
            throw new RuntimeException("You can't modify the empty runtime context in the tests, sorry.");
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
