package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.runtime.cloudthreads.ThreadId;
import org.apache.http.HttpResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CompleterInvokeClient {
    CompletableFuture<HttpResponse> invokeStage(String fnId, ThreadId threadId, CompletionId stageId, Datum.Blob closure, List<Result> body);

}
