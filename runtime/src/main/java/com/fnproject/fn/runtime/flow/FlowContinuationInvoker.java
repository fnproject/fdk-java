package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.*;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import com.fnproject.fn.runtime.flow.blobs.BlobApiClient;
import com.fnproject.fn.runtime.flow.blobs.BlobResponse;
import com.fnproject.fn.runtime.flow.blobs.RemoteBlobApiClient;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.*;

import static com.fnproject.fn.runtime.flow.RemoteCompleterApiClient.*;


/**
 * Invoker that handles flow stages
 */
public final class FlowContinuationInvoker implements FunctionInvoker {

    private static final String DEFAULT_COMPLETER_BASE_URL = "http://completer-svc:8081";
    private static final String COMPLETER_BASE_URL = "COMPLETER_BASE_URL";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class URLCompleterClientFactory implements CompleterClientFactory {
        private final String completerBaseUrl;
        private transient CompleterClient client;

        URLCompleterClientFactory(String completerBaseUrl) {
            this.completerBaseUrl = completerBaseUrl;
        }

        @Override
        public synchronized CompleterClient get() {
            if (this.client == null) {
                this.client = new RemoteFlowApiClient(completerBaseUrl + "/v1",
                   new RemoteBlobApiClient(completerBaseUrl + "/blobs", new HttpClient()), new HttpClient());
            }
            return this.client;
        }
    }

    /**
     * Gets or creates the completer client factory; if it has been overridden, the parameter will be ignored
     *
     * @param completerBaseUrl the completer base URL to use if and when creating the factory
     */
    private static synchronized CompleterClientFactory getOrCreateCompleterClientFactory(String completerBaseUrl) {
        if (FlowRuntimeGlobals.getCompleterClientFactory() == null) {
            FlowRuntimeGlobals.setCompleterClientFactory(new URLCompleterClientFactory(completerBaseUrl));
        }
        return FlowRuntimeGlobals.getCompleterClientFactory();
    }


    /**
     * Invoke the function wrapped by this loader
     *
     * @param evt The function event
     * @return the function response
     */
    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        Optional<String> graphIdOption = evt.getHeaders().get(FLOW_ID_HEADER);

        final String completerBaseUrl = ctx.getRuntimeContext().getConfigurationByKey(COMPLETER_BASE_URL).orElse(DEFAULT_COMPLETER_BASE_URL);

        if (graphIdOption.isPresent()) {
            if (FlowRuntimeGlobals.getCompleterClientFactory() == null) {
                FlowRuntimeGlobals.setCompleterClientFactory(getOrCreateCompleterClientFactory(completerBaseUrl));
            }

            final FlowId flowId = new FlowId(graphIdOption.get());
            Flows.FlowSource attachedSource = new Flows.FlowSource() {
                Flow runtime;

                @Override
                public synchronized Flow currentFlow() {
                    if (runtime == null) {
                        runtime = new RemoteFlow(flowId);
                    }
                    return runtime;
                }
            };

            Flows.setCurrentFlowSource(attachedSource);

            FlowRuntimeGlobals.setCurrentCompletionId(evt.getHeaders().get(STAGE_ID_HEADER)
               .map(CompletionId::new)
               .orElse(null));

            try {
                return evt.consumeBody((is) -> {
                    try {
                        APIModel.InvokeStageRequest invokeStageRequest = objectMapper.readValue(is, APIModel.InvokeStageRequest.class);
                        HttpClient httpClient = new HttpClient();
                        RemoteBlobApiClient blobClient = new RemoteBlobApiClient(completerBaseUrl + "/blobs", httpClient);

                        if(invokeStageRequest.closure.contentType.equals(CONTENT_TYPE_JAVA_OBJECT)) {
                            BlobResponse closureResponse = blobClient.readBlob(flowId.getId(), invokeStageRequest.closure.blobId, (requestInputStream) -> {
                                try(ObjectInputStream objectInputStream = new ObjectInputStream(requestInputStream)) {
                                    return objectInputStream.readObject();
                                } catch (IOException e) {
                                    throw new FunctionInputHandlingException("Error reading continuation content", e);
                                } catch (ClassNotFoundException e) {
                                    throw new FunctionInputHandlingException("Error reading continuation content", e);
                                }
                            }, invokeStageRequest.closure.contentType);

                            Object continuation = closureResponse.data;

                            DispatchPattern matchingDispatchPattern = null;
                            for(DispatchPattern dp : Dispatchers.values()) {
                                if(dp.matches(continuation)) {
                                    matchingDispatchPattern = dp;
                                    break;
                                }
                            }

                            if(matchingDispatchPattern != null) {
                                if(invokeStageRequest.args != null) {
                                    if (matchingDispatchPattern.numArguments() != invokeStageRequest.args.size()) {
                                        throw new FunctionInputHandlingException("Number of arguments provided in InvokeStageRequest does not match the number required by the function type");
                                    }
                                }
                            } else {
                                throw new FunctionInputHandlingException("No functional interface type matches the supplied continuation class");
                            }

                            Object[] args;

                            if(invokeStageRequest.args != null) {

                                args = invokeStageRequest.args.stream().map(arg -> {

                                    APIModel.Datum result = arg.result;
                                    if (result instanceof APIModel.StageRefDatum) {

                                        APIModel.StageRefDatum stageRefDatum = (APIModel.StageRefDatum) result;
                                        return ((RemoteFlow) Flows.currentFlow()).createRemoteFlowFuture(new CompletionId(stageRefDatum.stageId));

                                    } else if (result instanceof APIModel.BlobDatum) {

                                        APIModel.Blob blob = ((APIModel.BlobDatum) result).blob;

                                        BlobResponse blobResponse = blobClient.readBlob(flowId.getId(), blob.blobId, (requestInputStream) -> {
                                            try (ObjectInputStream objectInputStream = new ObjectInputStream(requestInputStream)) {
                                                return objectInputStream.readObject();
                                            } catch (IOException e) {
                                                throw new FunctionInputHandlingException("Error reading continuation content", e);

                                            } catch (ClassNotFoundException e) {
                                                throw new FunctionInputHandlingException("Error reading continuation content", e);
                                            }
                                        }, blob.contentType);

                                        return blobResponse.data;

                                    } else if (result instanceof APIModel.EmptyDatum) {
                                        // EmptyDatum represents null
                                        return null;
                                    } else if (result instanceof APIModel.ErrorDatum) {
                                        APIModel.ErrorDatum errorDatum = (APIModel.ErrorDatum) result;
                                        switch(errorDatum.type) {
                                            case StageTimeout:
                                                return new StageTimeoutException(errorDatum.message);
                                            case StageLost:
                                                return new StageLostException(errorDatum.message);
                                            case StageFailed:
                                                return new StageInvokeFailedException(errorDatum.message);
                                            case FunctionTimeout:
                                                return new FunctionTimeoutException(errorDatum.message);
                                            case FunctionInvokeFailed:
                                                return new FunctionInvokeFailedException(errorDatum.message);
                                            case InvalidStageResponse:
                                                return new InvalidStageResponseException(errorDatum.message);
                                            default:
                                                return new PlatformException(errorDatum.message);
                                        }

                                    } else if (result instanceof APIModel.HTTPReqDatum) {
                                        throw new FunctionInputHandlingException("Unhandled datum type: HTTPReqDatum");
                                    } else if (result instanceof APIModel.HTTPRespDatum) {
                                        throw new FunctionInputHandlingException("Unhandled datum type: HTTPRespDatum");
                                    } else if (result instanceof APIModel.StateDatum) {
                                        throw new FunctionInputHandlingException("Unhandled datum type: StateDatum");
                                    } else {
                                        throw new FunctionInputHandlingException("Unknown datum type");
                                    }

                                }).toArray();

                            } else {
                                args = new Object[0];
                            }

                            OutputEvent result = invokeContinuation(blobClient, flowId, continuation, matchingDispatchPattern.getInvokeMethod(continuation), args);

                            return Optional.of(result);

                        } else {
                            throw new FunctionInputHandlingException("Content type of closure isn't a Java serialized object");
                        }

                    } catch (IOException e) {
                        throw new FunctionInputHandlingException("Error reading continuation content", e);
                    }
                });
            } finally {
                Flows.setCurrentFlowSource(null);
                FlowRuntimeGlobals.setCurrentCompletionId(null);
            }

        } else {
            Flows.FlowSource deferredSource = new Flows.FlowSource() {
                Flow runtime;

                @Override
                public synchronized Flow currentFlow() {
                    if (runtime == null) {
                        String functionId = evt.getAppName() + evt.getRoute();
                        CompleterClientFactory factory = getOrCreateCompleterClientFactory(completerBaseUrl);
                        final FlowId flowId = factory.get().createFlow(functionId);
                        runtime = new RemoteFlow(flowId);

                        InvocationListener flowInvocationListener = new InvocationListener() {
                            @Override
                            public void onSuccess() {
                                factory.get().commit(flowId);
                            }

                            public void onFailure() {
                                factory.get().commit(flowId);
                            }
                        };
                        ctx.addListener(flowInvocationListener);
                    }
                    return runtime;
                }
            };

            // Not a flow invocation
            Flows.setCurrentFlowSource(deferredSource);
            return Optional.empty();
        }
    }

    private OutputEvent invokeContinuation(BlobApiClient blobApiClient, FlowId flowId, Object instance, Method m, Object[] args) {
        Object result;
        try {
            m.setAccessible(true);
            result = m.invoke(instance, args);
        } catch (InvocationTargetException ite) {
            ite.printStackTrace(System.err);
            throw new InternalFunctionInvocationException(
               "Error invoking flows lambda",
               ite.getCause(),
               constructExceptionOutputEvent(ite.getCause(), blobApiClient, flowId)
            );
        } catch (Exception ex) {
            throw new PlatformException(ex);
        }

        try {
            if (result == null) {
                return constructEmptyOutputEvent();
            } else if (result instanceof FlowFuture) {
                if (!(result instanceof RemoteFlow.RemoteFlowFuture)) {
                    // TODO: bubble up as stage failed exception
                    throw new InternalFunctionInvocationException("Error handling function response", new IllegalStateException("Unsupported flow future type return by function"));
                }
                return constructStageRefOutputEvent((RemoteFlow.RemoteFlowFuture) result);
            } else {

                return constructJavaObjectOutputEvent(result, true, blobApiClient, flowId);
            }
        } catch (IOException e) {
            ResultSerializationException rse = new ResultSerializationException("Result returned by stage is not serializable: " + e.getMessage(), e);
            throw new InternalFunctionInvocationException(
               "Error handling response from flow stage lambda",
               rse,
               constructExceptionOutputEvent(rse, blobApiClient, flowId)
            );
        }
    }

    /**
     * We want to always return 200, despite success or failure, from a continuation response.
     * We don't want to trample on what the use wants from an ordinary function.
     */
    final static class ContinuationOutputEvent implements OutputEvent {
        private final boolean success;
        private final String contentType;
        private final Map<String, String> headers;
        private final byte[] body;

        private ContinuationOutputEvent(boolean success, String contentType, Map<String, String> headers, byte[] body) {
            this.success = success;
            this.contentType = contentType;
            this.headers = headers;
            this.body = body;
        }

        /**
         * The completer expects a 200 on the output event.
         *
         * @return
         */
        @Override
        public int getStatusCode() {
            return OutputEvent.SUCCESS;
        }

        /**
         * The outer wrapper doesn't need to provide this: it's elided at the Fn boundary
         *
         * @return
         */
        @Override
        public Optional<String> getContentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public void writeToOutput(OutputStream out) throws IOException {
            out.write(body);
        }

        public Headers getHeaders() {
            return Headers.fromMap(headers);
        }

        String getInternalContentType() {
            return contentType;
        }

        byte[] getContentBody() {
            return body;
        }
    }

    private static OutputEvent constructEmptyOutputEvent() {
        return new ContinuationOutputEvent(true, null, Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_EMPTY), new byte[0]);
    }

    private OutputEvent constructExceptionOutputEvent(Throwable t, BlobApiClient blobClient, FlowId flowId) {
       try {
           return constructJavaObjectOutputEvent(t, false, blobClient, flowId);
        } catch (IOException e) {
            throw new PlatformException("Unexpected error serializing wrapped exception");
        }
    }

    private OutputEvent constructStageRefOutputEvent(RemoteFlow.RemoteFlowFuture future) throws IOException {
        APIModel.InvokeStageResponse invokeStageResponse = new APIModel.InvokeStageResponse();
        APIModel.CompletionResult completionResult = new APIModel.CompletionResult();
        completionResult.successful = true;
        APIModel.StageRefDatum stageRefDatum = new APIModel.StageRefDatum();
        stageRefDatum.stageId = future.id();
        invokeStageResponse.result = completionResult;

        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        objectMapper.writeValue(bb, invokeStageResponse);

        return new ContinuationOutputEvent(true, "application/json", Collections.emptyMap(), bb.toByteArray());
    }

    private OutputEvent constructJavaObjectOutputEvent(Object obj, boolean success, BlobApiClient blobClient, FlowId flowId) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(512)) {
            try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {

                // Serialize object
                out.writeObject(obj);

                // Write blob to store
                APIModel.Blob blobDatum = blobClient.writeBlob(flowId.getId(), baos.toByteArray(), CONTENT_TYPE_JAVA_OBJECT);

                // Build response
                APIModel.InvokeStageResponse invokeStageResponse = new APIModel.InvokeStageResponse();
                APIModel.CompletionResult completionResult = new APIModel.CompletionResult();

                completionResult.successful = success;
                APIModel.BlobDatum outputBlob = new APIModel.BlobDatum();
                outputBlob.blob = blobDatum;
                completionResult.result = outputBlob;
                invokeStageResponse.result = completionResult;

                // Convert response to JSON
                ByteArrayOutputStream bb = new ByteArrayOutputStream();
                objectMapper.writeValue(bb, invokeStageResponse);

                return new ContinuationOutputEvent(success, "application/json", Collections.emptyMap(), bb.toByteArray());
            }
        }
    }

    private interface DispatchPattern {
        boolean matches(Object instance);

        int numArguments();

        Method getInvokeMethod(Object instance);
    }

    /**
     * Calling conventions for different target objects
     */
    private enum Dispatchers implements DispatchPattern {
        CallableDispatch(Callable.class, 0, "call"),
        FunctionDispatch(Function.class, 1, "apply"),
        BiFunctionDispatch(BiFunction.class, 2, "apply"),
        RunnableDispatch(Runnable.class, 0, "run"),
        ConsumerDispatch(Consumer.class, 1, "accept"),
        BiConsumerDispatch(BiConsumer.class, 2, "accept"),
        SupplierDispatch(Supplier.class, 0, "get");


        @Override
        public boolean matches(Object instance) {
            return matchType.isInstance(instance);
        }

        public int numArguments() {
            return numArguments;
        }

        public Method getInvokeMethod(Object instance) {
            try {
                Class<?> args[] = new Class[numArguments];
                for (int i = 0; i < args.length; i++) {
                    args[i] = Object.class;
                }
                return instance.getClass().getMethod(methodName, args);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to find method " + methodName + " on " + instance.getClass());
            }
        }

        private final Class<?> matchType;
        private final int numArguments;
        private final String methodName;

        Dispatchers(Class<?> matchType, int numArguments, String methodName) {
            this.matchType = matchType;
            this.numArguments = numArguments;
            this.methodName = methodName;
        }
    }
}
