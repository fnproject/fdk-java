package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.HttpMethod;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    FlowId createThread(String functionId);

    CompletionId supply(FlowId flowID, Serializable code);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId thenApply(FlowId flowID, CompletionId completionId, Serializable consumer);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId whenComplete(FlowId flowID, CompletionId completionId, Serializable consumer);

    /**
     * Compose a function into the tree
     * The transmitted function is wrapped to convert th ElvisFuture into it's completion iD
     *
     * @return a completion ID that completes when the completion returned by the inner function completes
     */
    CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable fn);

    // block (indefinitely) until the completion completes
    Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader loader);

    // block until the timeout for the completion to complete and throw a TimeoutException upon reaching timeout
    Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader loader, long timeout, TimeUnit unit) throws TimeoutException;

    CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable fn);

    CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable fn);

    CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn);

    CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn);

    CompletionId anyOf(FlowId flowId, List<CompletionId> cids);

    CompletionId delay(FlowId flowId, long l);

    CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn);

    ExternalCompletion createExternalCompletion(FlowId flowId);

    CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers);

    CompletionId completedValue(FlowId flowId, Serializable value);


    CompletionId allOf(FlowId flowId, List<CompletionId> cids);

    CompletionId handle(FlowId flowId, CompletionId completionId, Serializable fn);

    CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable fn);

    CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable fn, CompletionId alternate);

    void commit(FlowId flowId);

    void addTerminationHook(FlowId flowId, Serializable code);

}
