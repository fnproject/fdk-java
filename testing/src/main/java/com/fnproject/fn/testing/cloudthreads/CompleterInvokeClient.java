package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.runtime.cloudthreads.ThreadId;

import java.util.List;

public interface CompleterInvokeClient {
    Result invokeStage(String fnId, ThreadId threadId, CompletionId stageId, Datum.Blob closure, List<Result> body);

}
