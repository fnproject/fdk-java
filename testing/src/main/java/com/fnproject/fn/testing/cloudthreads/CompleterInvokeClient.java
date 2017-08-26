package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CompleterInvokeClient {
    CompletableFuture<Result> invokeStage(String fnId, CompletionId stageId, Datum.Blob closure, List<Result> args);

}
