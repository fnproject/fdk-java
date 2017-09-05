package com.fnproject.fn.runtime.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Internal interface to a remote completion service
 */
public interface CompleterClient {


    interface ExternalCompletion {

        CompletionId completionId();
        URI completeURI();
        URI failureURI();

    }
    /**
     * create a new thread against the completer
     *
     * @param functionId
     * @return
     */
    ThreadId createThread(String functionId);

    CompletionId supply(ThreadId threadID, Serializable code);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId thenApply(ThreadId threadID, CompletionId completionId, Serializable consumer);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId whenComplete(ThreadId threadID, CompletionId completionId, Serializable consumer);

    /**
     * Compose a function into the tree
     * The transmitted function is wrapped to convert th ElvisFuture into it's completion iD
     *
     * @return a completion ID that completes when the completion returned by the inner function completes
     */
    CompletionId thenCompose(ThreadId threadId, CompletionId completionId, Serializable fn);

    // block (indefinitely) until the completion completes
    Object waitForCompletion(ThreadId threadID, CompletionId id, ClassLoader loader);

    CompletionId thenAccept(ThreadId threadId, CompletionId completionId, Serializable fn);

    CompletionId thenRun(ThreadId threadId, CompletionId completionId, Serializable fn);

    CompletionId acceptEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn);

    CompletionId applyToEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn);

    CompletionId anyOf(ThreadId threadId, List<CompletionId> cids);

    CompletionId delay(ThreadId threadId, long l);

    CompletionId thenAcceptBoth(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn);

    ExternalCompletion createExternalCompletion(ThreadId threadId);

    CompletionId invokeFunction(ThreadId threadId, String functionId, byte[] data, HttpMethod method, Headers headers);

    CompletionId completedValue(ThreadId threadId, Serializable value);


    CompletionId allOf(ThreadId threadId, List<CompletionId> cids);

    CompletionId handle(ThreadId threadId, CompletionId completionId, Serializable fn);

    CompletionId exceptionally(ThreadId threadId, CompletionId completionId, Serializable fn);

    CompletionId thenCombine(ThreadId threadId, CompletionId completionId, Serializable fn, CompletionId alternate);

    void commit(ThreadId threadId);

}
