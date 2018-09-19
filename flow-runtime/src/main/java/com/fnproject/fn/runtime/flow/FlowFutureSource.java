package com.fnproject.fn.runtime.flow;

import com.fnproject.fn.api.flow.FlowFuture;

/**
 * Created on 27/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public interface FlowFutureSource {
    <V> FlowFuture<V> createFlowFuture(CompletionId completionId);
}
