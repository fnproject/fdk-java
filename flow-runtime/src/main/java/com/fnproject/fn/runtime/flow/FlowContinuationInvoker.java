package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fnproject.fn.api.*;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.*;

import static com.fnproject.fn.runtime.flow.RemoteFlowApiClient.CONTENT_TYPE_JAVA_OBJECT;


/**
 * Invoker that handles flow stages
 */
public final class FlowContinuationInvoker implements FunctionInvoker {

    private static final String DEFAULT_COMPLETER_BASE_URL = "http://completer-svc:8081";
    private static final String COMPLETER_BASE_URL = "COMPLETER_BASE_URL";
    public static final String FLOW_ID_HEADER = "Fnproject-FlowId";


    FlowContinuationInvoker() {

    }

    private static class URLCompleterClientFactory implements CompleterClientFactory {
        private final String completerBaseUrl;
        private transient CompleterClient completerClient;
        private transient BlobStoreClient blobClient;

        URLCompleterClientFactory(String completerBaseUrl) {
            this.completerBaseUrl = completerBaseUrl;
        }

        @Override
        public synchronized CompleterClient getCompleterClient() {
            if (this.completerClient == null) {
                this.completerClient = new RemoteFlowApiClient(completerBaseUrl + "/v1",
                  getBlobStoreClient(), new HttpClient());
            }
            return this.completerClient;
        }

        public synchronized BlobStoreClient getBlobStoreClient() {

            if (this.blobClient == null) {
                this.blobClient = new RemoteBlobStoreClient(completerBaseUrl + "/blobs", new HttpClient());
            }
            return this.blobClient;
        }


    }

    /**
     * Gets or creates the completer completerClient factory; if it has been overridden, the parameter will be ignored
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
            CompleterClientFactory ccf = getOrCreateCompleterClientFactory(completerBaseUrl);

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


            try {
                return evt.consumeBody((is) -> {
                    try {

                        APIModel.InvokeStageRequest invokeStageRequest = FlowRuntimeGlobals.getObjectMapper().readValue(is, APIModel.InvokeStageRequest.class);
                        HttpClient httpClient = new HttpClient();
                        BlobStoreClient blobClient = ccf.getBlobStoreClient();

                        FlowRuntimeGlobals.setCurrentCompletionId(new CompletionId(invokeStageRequest.stageId));

                        if (invokeStageRequest.closure.contentType.equals(CONTENT_TYPE_JAVA_OBJECT)) {
                            Object continuation = blobClient.readBlob(flowId.getId(), invokeStageRequest.closure.blobId, (requestInputStream) -> {
                                try (ObjectInputStream objectInputStream = new ObjectInputStream(requestInputStream)) {
                                    return objectInputStream.readObject();
                                } catch (IOException | ClassNotFoundException e) {
                                    throw new FunctionInputHandlingException("Error reading continuation content", e);
                                }
                            }, invokeStageRequest.closure.contentType);


                            DispatchPattern matchingDispatchPattern = null;
                            for (DispatchPattern dp : Dispatchers.values()) {
                                if (dp.matches(continuation)) {
                                    matchingDispatchPattern = dp;
                                    break;
                                }
                            }

                            if (matchingDispatchPattern != null) {
                                if (matchingDispatchPattern.numArguments() != invokeStageRequest.args.size()) {
                                    throw new FunctionInputHandlingException("Number of arguments provided (" + invokeStageRequest.args.size() + ") in .InvokeStageRequest does not match the number required by the function type (" + matchingDispatchPattern.numArguments() + ")");
                                }
                            } else {
                                throw new FunctionInputHandlingException("No functional interface type matches the supplied continuation class");
                            }

                            Object[] args = invokeStageRequest.args.stream().map(arg -> arg.toJava(flowId, blobClient, getClass().getClassLoader())).toArray();


                            OutputEvent result = invokeContinuation(blobClient, flowId, continuation, matchingDispatchPattern.getInvokeMethod(continuation), args);

                            return Optional.of(result);

                        } else {
                            throw new FunctionInputHandlingException("Content type of closure isn't a Java serialized object");
                        }

                    } catch (IOException e) {
                        throw new PlatformCommunicationException("Error reading continuation content", e);
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
                        String functionId = ctx.getRuntimeContext().getFunctionID();
                        CompleterClientFactory factory = getOrCreateCompleterClientFactory(completerBaseUrl);
                        final FlowId flowId = factory.getCompleterClient().createFlow(functionId);
                        runtime = new RemoteFlow(flowId);

                        InvocationListener flowInvocationListener = new InvocationListener() {
                            @Override
                            public void onSuccess() {
                                factory.getCompleterClient().commit(flowId);
                            }

                            public void onFailure() {
                                factory.getCompleterClient().commit(flowId);
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

    private OutputEvent invokeContinuation(BlobStoreClient blobStoreClient, FlowId flowId, Object instance, Method m, Object[] args) {
        Object result;
        try {
            m.setAccessible(true);
            result = m.invoke(instance, args);
        } catch (InvocationTargetException ite) {
            APIModel.Datum datum = APIModel.datumFromJava(flowId, ite.getCause(), blobStoreClient);

            throw new InternalFunctionInvocationException(
              "Error invoking flows lambda",
              ite.getCause(),
              constructOutputEvent(datum, false)
            );
        } catch (Exception ex) {
            throw new PlatformException(ex);
        }

        APIModel.Datum resultDatum = APIModel.datumFromJava(flowId, result, blobStoreClient);
        return constructOutputEvent(resultDatum, true);

    }

    /**
     * We want to always return 200, despite success or failure, from a continuation response.
     * We don't want to trample on what the use wants from an ordinary function.
     */
    final static class ContinuationOutputEvent implements OutputEvent {
        private final byte[] body;
        private static final Headers headers = Headers.emptyHeaders().setHeader(OutputEvent.CONTENT_TYPE_HEADER, "application/json");

        private ContinuationOutputEvent(boolean success, byte[] body) {
            this.body = body;
        }

        @Override
        public Status getStatus() {
            return Status.Success;
        }


        @Override
        public void writeToOutput(OutputStream out) throws IOException {
            out.write(body);
        }

        @Override
        public Headers getHeaders() {
            return headers;
        }
    }

    private OutputEvent constructOutputEvent(APIModel.Datum obj, boolean success) {
        APIModel.CompletionResult result = new APIModel.CompletionResult();
        result.result = obj;
        result.successful = success;

        APIModel.InvokeStageResponse resp = new APIModel.InvokeStageResponse();
        resp.result = result;

        String json;
        try {
            json = FlowRuntimeGlobals.getObjectMapper().writeValueAsString(resp);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Error writing JSON", e);
        }

        return new ContinuationOutputEvent(success, json.getBytes());

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
