package com.fnproject.fn.runtime.cloudthreads;

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
 * REST cloud threads runtime
 * <p>
 * This
 */
public final class RemoteFlow implements Flow, Serializable {
    private transient CompleterClient client;
    private final ThreadId threadId;

    RemoteFlow(ThreadId thread) {
        this.threadId = Objects.requireNonNull(thread);
    }

    private CompleterClient getClient() {
        if (client == null) {
            client = CloudThreadsRuntimeGlobals.getCompleterClientFactory().get();
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

    class RemoteFlowFuture<T> implements FlowFuture<T>, Serializable {
        private final CompletionId completionId;

        RemoteFlowFuture(CompletionId completionId) {
            this.completionId = Objects.requireNonNull(completionId, "completionId");
        }

        @Override
        public <U> FlowFuture<U> thenApply(Flows.SerFunction<T, U> fn) {
            CompletionId newcid = getClient().thenApply(threadId, completionId, fn);
            return new RemoteFlowFuture<>(newcid);
        }

        @Override
        public <X> FlowFuture<X> thenCompose(Flows.SerFunction<T, FlowFuture<X>> fn) {
            CompletionId newCid = getClient().thenCompose(threadId, completionId, fn);
            return new RemoteFlowFuture<>(newCid);
        }

        @Override
        public FlowFuture<T> whenComplete(Flows.SerBiConsumer<T, Throwable> fn) {
            return new RemoteFlowFuture<>(getClient().whenComplete(threadId, completionId, fn));
        }

        @Override
        public FlowFuture<Void> thenAccept(Flows.SerConsumer<T> fn) {
            return new RemoteFlowFuture<>(getClient().thenAccept(threadId, completionId, fn));
        }

        @Override
        public FlowFuture<Void> acceptEither(FlowFuture<? extends T> alt, Flows.SerConsumer<T> fn) {
            return new RemoteFlowFuture<>(getClient().acceptEither(threadId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn));
        }

        @Override
        public <X> FlowFuture<X> applyToEither(FlowFuture<? extends T> alt, Flows.SerFunction<T, X> fn) {
            return new RemoteFlowFuture<>(getClient().applyToEither(threadId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn));
        }

        @Override
        public <X> FlowFuture<Void> thenAcceptBoth(FlowFuture<X> alt, Flows.SerBiConsumer<T, X> fn) {
            return new RemoteFlowFuture<>(getClient().thenAcceptBoth(threadId, completionId, ((RemoteFlowFuture<?>) alt).completionId, fn));
        }

        @Override
        public FlowFuture<Void> thenRun(Flows.SerRunnable fn) {
            return new RemoteFlowFuture<>(getClient().thenRun(threadId, completionId, fn));
        }

        @Override
        public <X> FlowFuture<X> handle(Flows.SerBiFunction<? super T, Throwable, ? extends X> fn) {
            return new RemoteFlowFuture<>(getClient().handle(threadId, completionId, fn));
        }

        @Override
        public FlowFuture<T> exceptionally(Flows.SerFunction<Throwable, ? extends T> fn) {
            return new RemoteFlowFuture<>(getClient().exceptionally(threadId, completionId, fn));
        }

        @Override
        public <U, X> FlowFuture<X> thenCombine(FlowFuture<? extends U> other, Flows.SerBiFunction<? super T, ? super U, ? extends X> fn) {
            return new RemoteFlowFuture<>(getClient().thenCombine(threadId, completionId, fn, ((RemoteFlowFuture<?>) other).completionId));
        }

        @Override
        public T get() {
            return (T) getClient().waitForCompletion(threadId, completionId, getClass().getClassLoader());
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws TimeoutException {
           return (T) getClient().waitForCompletion(threadId, completionId, getClass().getClassLoader(), timeout, unit);
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
        CompletionId cid = getClient().invokeFunction(threadId, functionId, data, method, headers);
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public <T> FlowFuture<T> supply(Flows.SerCallable<T> c) {
        CompletionId cid = getClient().supply(threadId, c);
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Void> supply(Flows.SerRunnable runnable) {
        CompletionId cid = getClient().supply(threadId, runnable);
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Void> delay(long i, TimeUnit tu) {
        if (i < 0) {
            throw new IllegalArgumentException("Delay value must be non-negative");
        }
        CompletionId cid = getClient().delay(threadId, tu.toMillis(i));
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public <T extends Serializable> FlowFuture<T> completedValue(T value) {
        return new RemoteFlowFuture<>(getClient().completedValue(threadId, value));
    }

    @Override
    public ExternalFlowFuture<HttpRequest> createExternalFuture() {
        CompleterClient.ExternalCompletion ext = getClient().createExternalCompletion(threadId);
        return new RemoteExternalFlowFuture<>(ext.completionId(), ext.completeURI(), ext.failureURI());
    }

    @Override
    public FlowFuture<Void> allOf(FlowFuture<?>... flowFutures) {
        if (flowFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(flowFutures).map((cf) -> ((RemoteFlowFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().allOf(threadId, cids);
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public FlowFuture<Object> anyOf(FlowFuture<?>... flowFutures) {
        if (flowFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(flowFutures).map((cf) -> ((RemoteFlowFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().anyOf(threadId, cids);
        return new RemoteFlowFuture<>(cid);
    }

    @Override
    public Flow addTerminationHook(Flows.SerConsumer<CloudThreadState> hook) {
        getClient().addTerminationHook(threadId, hook);
        return this;
    }
}


