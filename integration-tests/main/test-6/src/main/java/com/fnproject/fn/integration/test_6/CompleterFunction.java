package com.fnproject.fn.integration.test_6;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        try {
            return rt.supply(() -> {
                Thread.sleep(10000);
                return 42;
            }).get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            System.err.println("Caught timeout");
            return 20;
        }
    }

}
