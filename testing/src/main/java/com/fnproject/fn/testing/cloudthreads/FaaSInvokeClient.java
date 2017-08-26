package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FaaSInvokeClient {
    CompletableFuture<Result> invokeStage(String fnId, CompletionId stageId, Datum.Blob closure, List<Result> args);

    CompletableFuture<Result> invokeFunction(String fnId, HttpMethod method, Headers headers, Datum.Blob data);
}
