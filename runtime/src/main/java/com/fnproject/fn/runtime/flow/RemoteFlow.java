package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * REST flows runtime
 * <p>
 * This
 */
public final class RemoteFlow implements Flow, Serializable {
    private transient CompleterClient client;
    private final FlowId flowId;

    RemoteFlow(FlowId flowId) {
        this.flowId = Objects.requireNonNull(flowId);
    }

    private CompleterClient getClient() {
        if (client == null) {
            client = FlowRuntimeGlobals.getCompleterClientFactory().get();
        }
        return client;
    }

    private class RemoteExternalFlowFuture<T> extends RemoteFlowFuture<T> implements ExternalFlowFuture<T> {
        private final URI completionUri;
        private final URI failureUri;

        private RemoteExternalFlowFuture(CompletionId completionId, URI completionUri, URI failureUri) {
            super(completionId);
            this.completionUri = completionUri;
            this.failureUri = failureUri;
        }


        @Override
        public URI completionUrl() {
            return completionUri;
        }

        @Override
        public URI failUrl() {
            return failureUri;
        }
    }

    RemoteFlowFuture createRemoteFlowFuture(CompletionId completionId) {
        return new RemoteFlowFuture(completionId);
    }

    class RemoteFlowFuture<T> implements FlowFuture<T>, Serializable {
        private final CompletionId completionId;

        RemoteFlowFuture(CompletionId completionId) {
            this.completionId = Objects.requireNonNull(completionId, "completionId");
        }

        @Override
        public <U> FlowFuture<U> thenApply(Flows.SerFunction<T, U> fn) {
            CompletionId newcid = getClient().thenApply(flowId, completionId, fn, CodeLocation.fromCallerLocation(1));
            return new RemoteFlowFuture<>(newcid);
        }

        @Override
        public <X> FlowFuture<X> thenCompose(Flows.SerFunction<T, FlowFuture<X>> fn) {
            CompletionId newCid = getClient().thenCompose(flowId, completionId, fn, CodeLocation.fromCallerLocation(1));
            return new RemoteFlowFuture<>(newCid);
        }

        @Override
        public FlowFuture<T> exceptionallyCompose(Flows.SerFunction<Throwable, FlowFuture<T>> fn) {
            return new RemoteFlowFuture<>(getClient().exceptionallyCompose(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public FlowFuture<T> whenComplete(Flows.SerBiConsumer<T, Throwable> fn) {
            return new RemoteFlowFuture<>(getClient().whenComplete(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public FlowFuture<Void> thenAccept(Flows.SerConsumer<T> fn) {
            return new RemoteFlowFuture<>(getClient().thenAccept(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public FlowFuture<Void> acceptEither(FlowFuture<? extends T> alt, Flows.SerConsumer<T> fn) {
            return new RemoteFlowFuture<>(getClient().acceptEither(flowId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public <X> FlowFuture<X> applyToEither(FlowFuture<? extends T> alt, Flows.SerFunction<T, X> fn) {
            return new RemoteFlowFuture<>(getClient().applyToEither(flowId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public <X> FlowFuture<Void> thenAcceptBoth(FlowFuture<X> alt, Flows.SerBiConsumer<T, X> fn) {
            return new RemoteFlowFuture<>(getClient().thenAcceptBoth(flowId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public FlowFuture<Void> thenRun(Flows.SerRunnable fn) {
            return new RemoteFlowFuture<>(getClient().thenRun(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public <X> FlowFuture<X> handle(Flows.SerBiFunction<? super T, Throwable, ? extends X> fn) {
            return new RemoteFlowFuture<>(getClient().handle(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public FlowFuture<T> exceptionally(Flows.SerFunction<Throwable, ? extends T> fn) {
            return new RemoteFlowFuture<>(getClient().exceptionally(flowId, completionId, fn, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public <U, X> FlowFuture<X> thenCombine(FlowFuture<? extends U> other, Flows.SerBiFunction<? super T, ? super U, ? extends X> fn) {
            return new RemoteFlowFuture<>(getClient().thenCombine(flowId, completionId, fn, ((RemoteFlowFuture<?>) other).completionId, CodeLocation.fromCallerLocation(1)));
        }

        @Override
        public T get() {
            return (T) getClient().waitForCompletion(flowId, completionId, getClass().getClassLoader());
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws TimeoutException {
            return (T) getClient().waitForCompletion(flowId, completionId, getClass().getClassLoader(), timeout, unit);
        }

        @Override
        public T getNow(T valueIfAbsent) {
            try {
                return get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                return valueIfAbsent;
            }
        }

        public String id() {
            return completionId.getId();
        }

    }

    @Override
    public FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data) {
        CompletionId cid = getClient().invokeFunction(flowId, functionId, data, method, headers, CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public <T extends Serializable, U> FlowFuture<T> invokeFunction(String functionId, HttpMethod method, Headers headers, U input, Class<T> responseType) {
        return JsonInvoke.invokeFunction(this,functionId,method,headers,input,responseType);
    }

    @Override
    public <U> FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, U input) {
        return JsonInvoke.invokeFunction(this,functionId,method,headers,input);
    }


    @Override
    public <T> FlowFuture<T> supply(Flows.SerCallable<T> c) {
        CompletionId cid = getClient().supply(flowId, c, CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Void> supply(Flows.SerRunnable runnable) {
        CompletionId cid = getClient().supply(flowId, runnable, CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Void> delay(long i, TimeUnit tu) {
        if (i < 0) {
            throw new IllegalArgumentException("Delay value must be non-negative");
        }
        CompletionId cid = getClient().delay(flowId, tu.toMillis(i), CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public <T> FlowFuture<T> completedValue(T value) {
        return new RemoteFlowFuture<>(getClient().completedValue(flowId, true, value, CodeLocation.fromCallerLocation(1)));
    }

    @Override
    public <T> FlowFuture<T> failedFuture(Throwable ex) {
        return new RemoteFlowFuture<>(getClient().completedValue(flowId, false, ex, CodeLocation.fromCallerLocation(1)));
    }

    @Override
    public ExternalFlowFuture<HttpRequest> createExternalFuture() {
        CompleterClient.ExternalCompletion ext = getClient().createExternalCompletion(flowId, CodeLocation.fromCallerLocation(1));
        return new RemoteExternalFlowFuture<>(ext.completionId(), ext.completeURI(), ext.failureURI());
    }

    @Override
    public FlowFuture<Void> allOf(FlowFuture<?>... flowFutures) {
        if (flowFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(flowFutures).map((cf) -> ((RemoteFlowFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().allOf(flowId, cids, CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Object> anyOf(FlowFuture<?>... flowFutures) {
        if (flowFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(flowFutures).map((cf) -> ((RemoteFlowFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().anyOf(flowId, cids, CodeLocation.fromCallerLocation(1));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public Flow addTerminationHook(Flows.SerConsumer<FlowState> hook) {
        getClient().addTerminationHook(flowId, hook, CodeLocation.fromCallerLocation(1));
        return this;
    }
}


