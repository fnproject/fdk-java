package com.fnproject.fn.integration.test_5;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        rt.addTerminationHook( (ignored) -> { System.err.println("Ran the hook."); });
        return rt.supply(() -> { Thread.sleep(1000); return 42; }).get();
    }

}
