package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.*;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
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

    private static class URLCompleterClientFactory implements CompleterClientFactory {
        private final String completerBaseUrl;
        private transient CompleterClient client;

        URLCompleterClientFactory(String completerBaseUrl) {
            this.completerBaseUrl = completerBaseUrl;
        }

        @Override
        public synchronized CompleterClient get() {
            if (this.client == null) {
                this.client = new RemoteCompleterApiClient(completerBaseUrl, new HttpClient());
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
                    SerUtils.ContentStream cs ;
                    try {
                        cs = new SerUtils.ContentStream(
                                evt.getHeaders().get(CONTENT_TYPE_HEADER)
                                        .orElseThrow(() -> new RuntimeException(CONTENT_TYPE_HEADER + " header not present, expected value to multipart/form-data")),
                                is);
                    } catch (IOException e) {
                        throw new FunctionInputHandlingException("Error reading continuation content", e);
                    }
                    Object continuation;
                    try {
                        continuation = cs.readObject(DATUM_TYPE_BLOB);
                    } catch (IOException e) {
                        throw new FunctionInputHandlingException("Error reading continuation content ", e);
                    } catch (ClassNotFoundException e) {
                        throw new FunctionInputHandlingException("Unable to extract closure object", e);
                    } catch (SerUtils.Deserializer.DeserializeException e) {
                        throw new FunctionInputHandlingException("Error deserializing closure object", e);
                    }
                    for (DispatchPattern dp : Dispatchers.values()) {
                        if (dp.matches(continuation)) {
                            Object[] args = new Object[dp.numArguments()];
                            for (int i = 0; i < args.length; i++) {
                                try {
                                    args[i] = cs.readObject();
                                } catch (IOException e) {
                                    throw new FunctionInputHandlingException("Error reading continuation content ", e);
                                } catch (ClassNotFoundException e) {
                                    throw new FunctionInputHandlingException("Unable to extract closure argument", e);
                                } catch (SerUtils.Deserializer.DeserializeException e) {
                                    throw new FunctionInputHandlingException("Error deserializing closure argument", e);
                                }
                            }
                            OutputEvent result = invokeContinuation(continuation, dp.getInvokeMethod(continuation), args);
                            return Optional.of(result);
                        }
                    }

                    throw new IllegalStateException("Invalid invocation - no dispatch mechanism found for " + continuation.getClass());
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
                        String functionId = evt.getAppName() + evt.getPath();
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

    private static OutputEvent invokeContinuation(Object instance, Method m, Object[] args) {
        Object result;
        try {
            m.setAccessible(true);
            result = m.invoke(instance, args);
        } catch (InvocationTargetException ite) {
            ite.printStackTrace(System.err);
            throw new InternalFunctionInvocationException(
                    "Error invoking flows lambda",
                    ite.getCause(),
                    constructExceptionOutputEvent(ite.getCause())
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
                return constructJavaObjectOutputEvent(result);
            }
        } catch (IOException e) {
            ResultSerializationException rse = new ResultSerializationException("Result returned by stage is not serializable: " + e.getMessage(), e);
            throw new InternalFunctionInvocationException(
                    "Error handling response from flow stage lambda",
                    rse,
                    constructExceptionOutputEvent(rse)
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
            return Optional.empty();
        }

        /**
         * We have no way of passing out headers through the functions platform.
         * Instead, we put the additional headers we require in the *body* (ie, the entity becomes
         * a single MIME-encoded entity (that is, an HTTP message *including* the status line).
         */
        @Override
        public void writeToOutput(OutputStream out) throws IOException {
            out.write("HTTP/1.1 200\r\n".getBytes());   // The completer wants this.

            addHeader(out, RESULT_STATUS_HEADER, success ? RESULT_STATUS_SUCCESS : RESULT_STATUS_FAILURE);

            if (contentType != null) {
                addHeader(out, CONTENT_TYPE_HEADER, contentType);
            }
            for (Map.Entry<String, String> h : headers.entrySet()) {
                addHeader(out, h.getKey(), h.getValue());
            }

            addHeader(out, "Content-Length", Long.toString(body.length));
            out.write(new byte[]{'\r', '\n'});

            out.write(body);
        }

        private void addHeader(OutputStream out, String name, String value) throws IOException {
            out.write(name.getBytes());
            out.write(new byte[]{':', ' '});
            out.write(value.getBytes());
            out.write(new byte[]{'\r', '\n'});
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

    private static OutputEvent constructExceptionOutputEvent(Throwable t) {
        byte[] serializedException;
        try {
            serializedException = SerUtils.serialize(t);
        } catch (IOException ignored) {
            // do we need to do this? - can we use the FaaS Error types to construct these Exceptions?
            try {
                serializedException = SerUtils.serialize(new WrappedFunctionException(t));
            } catch (IOException e) {
                throw new PlatformException("Unexpected error serializing wrapped exception");
            }
        }
        return new ContinuationOutputEvent(false, CONTENT_TYPE_JAVA_OBJECT, Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB), serializedException);
    }

    private static OutputEvent constructStageRefOutputEvent(RemoteFlow.RemoteFlowFuture future) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DATUM_TYPE_HEADER, DATUM_TYPE_STAGEREF);
        headers.put(STAGE_ID_HEADER, future.id());
        return new ContinuationOutputEvent(true, null, headers, new byte[0]);
    }

    private static OutputEvent constructJavaObjectOutputEvent(Object obj) throws IOException {
        byte[] serializedObject = SerUtils.serialize(obj);
        return new ContinuationOutputEvent(true, CONTENT_TYPE_JAVA_OBJECT, Collections.singletonMap(DATUM_TYPE_HEADER, DATUM_TYPE_BLOB), serializedObject);
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
