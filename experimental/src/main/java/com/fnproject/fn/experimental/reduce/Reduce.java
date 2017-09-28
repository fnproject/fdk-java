package com.fnproject.fn.experimental.reduce;

import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.util.Arrays;
import java.util.List;

public class Reduce {
    public static <X> FlowFuture<List<FlowFuture<X>>> allResults(FlowFuture<X>... flows) {
        return Flows.currentFlow().allOf(flows).thenApply(ignored -> Arrays.asList(flows));
    }
}
