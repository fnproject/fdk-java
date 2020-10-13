/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.HttpMethod;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Internal interface to a remote completion service
 */
public interface CompleterClient {

    /**
     * create a new flow against the flow service
     *
     * @param functionId
     * @return
     */
    FlowId createFlow(String functionId);

    CompletionId supply(FlowId flowID, Serializable code, CodeLocation codeLocation);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId thenApply(FlowId flowID, CompletionId completionId, Serializable consumer, CodeLocation codeLocation);

    // thenApply completionID   -> DO NOW  (result) | new parentId
    CompletionId whenComplete(FlowId flowID, CompletionId completionId, Serializable consumer, CodeLocation codeLocation);

    /**
     * Compose a function into the tree
     * The transmitted function is wrapped to convert th ElvisFuture into it's completion iD
     *
     * @return a completion ID that completes when the completion returned by the inner function completes
     */
    CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    // block (indefinitely) until the completion completes
    Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader loader);

    // block until the timeout for the completion to complete and throw a TimeoutException upon reaching timeout
    Object waitForCompletion(FlowId flowId, CompletionId id, ClassLoader loader, long timeout, TimeUnit unit) throws TimeoutException;

    CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation);

    CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation);

    boolean complete(FlowId flowId, CompletionId completionId, Object value, CodeLocation codeLocation);

    boolean completeExceptionally(FlowId flowId, CompletionId completionId, Throwable value, CodeLocation codeLocation);

    CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation);

    CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation);

    CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable fn, CodeLocation codeLocation);

    CompletionId createCompletion(FlowId flowId, CodeLocation codeLocation);

    CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation);

    CompletionId completedValue(FlowId flowId, boolean success, Object value, CodeLocation codeLocation);

    CompletionId allOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation);

    CompletionId handle(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    CompletionId exceptionallyCompose(FlowId flowId, CompletionId completionId, Serializable fn, CodeLocation codeLocation);

    CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable fn, CompletionId alternate, CodeLocation codeLocation);

    void commit(FlowId flowId);

    void addTerminationHook(FlowId flowId, Serializable code, CodeLocation codeLocation);

}
