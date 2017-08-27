package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import org.apache.http.HttpResponse;

import java.util.concurrent.CompletableFuture;

public interface CompleterInvokeClient {
    CompletableFuture<HttpResponse> invokeStage(String fnId, CompletionId stageId,byte[] body);

}
