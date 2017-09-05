package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;

import java.util.concurrent.CompletableFuture;

public interface FnInvokeClient {
    CompletableFuture<Result> invokeFunction(String fnId, HttpMethod method, Headers headers, byte[] data);
}
