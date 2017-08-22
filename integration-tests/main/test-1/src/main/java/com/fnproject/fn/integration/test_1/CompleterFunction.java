package com.fnproject.fn.integration.test_1;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        return rt.supply(() -> Integer.parseInt(input))
                .thenApply((i) -> i + 3)
                .get();
    }
}
