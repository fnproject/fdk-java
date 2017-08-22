package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import java.io.Serializable;

public class CloudThreadsFn implements Serializable {

    public static void usingCloudThreads() {
        CloudThreads.currentRuntime();
    }

    public static void notUsingCloudThreads() {
    }

    public static void supply() {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        rt.supply(() -> 3);
    }

    public static void accessRuntimeMultipleTimes() {
        CloudThreads.currentRuntime();
        CloudThreads.currentRuntime();
    }

    public static Integer supplyAndGetResult() {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        Integer res =  rt.supply(() -> 3).get();

        return res;
    }
}

