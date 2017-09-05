package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.*;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * REST cloud threads runtime
 * <p>
 * This
 */
public final class RemoteCloudThreadRuntime implements CloudThreadRuntime, Serializable {
    private transient CompleterClient client;
    private final ThreadId threadId;

    RemoteCloudThreadRuntime(ThreadId thread) {
        this.threadId = Objects.requireNonNull(thread);
    }

    private CompleterClient getClient() {
        if (client == null) {
            client = CloudThreadsRuntimeGlobals.getCompleterClientFactory().get();
        }
        return client;
    }

    private class RemoteExternalCloudFuture<T> extends RemoteCloudFuture<T> implements ExternalCloudFuture<T> {
        private final URI completionUri;
        private final URI failureUri;

        private RemoteExternalCloudFuture(CompletionId completionId, URI completionUri, URI failureUri) {
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

   class RemoteCloudFuture<T> implements CloudFuture<T>, Serializable {
        private final CompletionId completionId;

        RemoteCloudFuture(CompletionId completionId) {
            this.completionId = Objects.requireNonNull(completionId, "completionId");
        }

        @Override
        public <U> CloudFuture<U> thenApply(CloudThreads.SerFunction<T, U> fn) {
            CompletionId newcid = getClient().thenApply(threadId, completionId, fn);
            return new RemoteCloudFuture<>(newcid);
        }

        @Override
        public <X> CloudFuture<X> thenCompose(CloudThreads.SerFunction<T, CloudFuture<X>> fn) {
            CompletionId newCid = getClient().thenCompose(threadId, completionId, fn);
            return new RemoteCloudFuture<>(newCid);
        }

        @Override
        public CloudFuture<T> whenComplete(CloudThreads.SerBiConsumer<T, Throwable> fn) {
            return new RemoteCloudFuture<>(getClient().whenComplete(threadId, completionId, fn));
        }

        @Override
        public CloudFuture<Void> thenAccept(CloudThreads.SerConsumer<T> fn) {
            return new RemoteCloudFuture<>(getClient().thenAccept(threadId, completionId, fn));
        }

        @Override
        public CloudFuture<Void> acceptEither(CloudFuture<? extends T> alt, CloudThreads.SerConsumer<T> fn) {
            return new RemoteCloudFuture<>(getClient().acceptEither(threadId, completionId, ((RemoteCloudFuture<?>) alt).completionId, fn));
        }

        @Override
        public <X> CloudFuture<X> applyToEither(CloudFuture<? extends T> alt, CloudThreads.SerFunction<T, X> fn) {
            return new RemoteCloudFuture<>(getClient().applyToEither(threadId, completionId, ((RemoteCloudFuture<?>) alt).completionId, fn));
        }

        @Override
        public <X> CloudFuture<Void> thenAcceptBoth(CloudFuture<X> alt, CloudThreads.SerBiConsumer<T, X> fn) {
            return new RemoteCloudFuture<>(getClient().thenAcceptBoth(threadId, completionId, ((RemoteCloudFuture<?>) alt).completionId, fn));
        }


        @Override
        public CloudFuture<Void> thenRun(CloudThreads.SerRunnable fn) {
            return new RemoteCloudFuture<>(getClient().thenRun(threadId, completionId, fn));
        }

        @Override
        public <X> CloudFuture<X> handle(CloudThreads.SerBiFunction<? super T, Throwable, ? extends X> fn) {
            return new RemoteCloudFuture<>(getClient().handle(threadId, completionId, fn));
        }

        @Override
        public CloudFuture<T> exceptionally(CloudThreads.SerFunction<Throwable, ? extends T> fn) {
            return new RemoteCloudFuture<>(getClient().exceptionally(threadId, completionId, fn));
        }

        @Override
        public <U, X> CloudFuture<X> thenCombine(CloudFuture<? extends U> other, CloudThreads.SerBiFunction<? super T, ? super U, ? extends X> fn) {
            return new RemoteCloudFuture<>(getClient().thenCombine(threadId, completionId, fn, ((RemoteCloudFuture<?>) other).completionId));
        }

        @Override
        public T get() {
            return (T) getClient().waitForCompletion(threadId, completionId, getClass().getClassLoader());
        }

        public String id() {
            return completionId.getId();
        }

    }

    @Override
    public CloudFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data) {
        CompletionId cid = getClient().invokeFunction(threadId, functionId, data, method, headers);
        return new RemoteCloudFuture<>(cid);
    }

    @Override
    public <T> CloudFuture<T> supply(CloudThreads.SerCallable<T> c) {
        CompletionId cid = getClient().supply(threadId, c);
        return new RemoteCloudFuture<>(cid);
    }

    @Override
    public CloudFuture<Void> supply(CloudThreads.SerRunnable runnable) {
        CompletionId cid = getClient().supply(threadId, runnable);
        return new RemoteCloudFuture<>(cid);
    }

    @Override
    public CloudFuture<Void> delay(long i, TimeUnit tu) {
        if (i < 0) {
            throw new IllegalArgumentException("Delay value must be non-negative");
        }
        CompletionId cid = getClient().delay(threadId, tu.toMillis(i));
        return new RemoteCloudFuture<>(cid);
    }

    @Override
    public <T extends Serializable> CloudFuture<T> completedValue(T value) {
        return new RemoteCloudFuture<>(getClient().completedValue(threadId, value));
    }

    @Override
    public ExternalCloudFuture<HttpRequest> createExternalFuture() {
        CompleterClient.ExternalCompletion ext = getClient().createExternalCompletion(threadId);
        return new RemoteExternalCloudFuture<>(ext.completionId(), ext.completeURI(), ext.failureURI());
    }

    @Override
    public CloudFuture<Void> allOf(CloudFuture<?>... cloudFutures) {
        if (cloudFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(cloudFutures).map((cf) -> ((RemoteCloudFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().allOf(threadId, cids);
        return new RemoteCloudFuture<>(cid);
    }

    @Override
    public CloudFuture<Object> anyOf(CloudFuture<?>... cloudFutures) {
        if (cloudFutures.length == 0) {
            throw new IllegalArgumentException("at least one future must be specified");
        }
        List<CompletionId> cids = Arrays.stream(cloudFutures).map((cf) -> ((RemoteCloudFuture<?>) cf).completionId).collect(Collectors.toList());
        CompletionId cid = getClient().anyOf(threadId, cids);
        return new RemoteCloudFuture<>(cid);
    }
}


