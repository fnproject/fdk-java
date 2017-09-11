package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

import java.io.Serializable;

public class CloudThreadsFn implements Serializable {

    public static void usingCloudThreads() {
        Flows.currentRuntime();
    }

    public static void notUsingCloudThreads() {
    }

    public static void supply() {
        Flow rt = Flows.currentRuntime();
        rt.supply(() -> 3);
    }

    public static void accessRuntimeMultipleTimes() {
        Flows.currentRuntime();
        Flows.currentRuntime();
    }

    public static Integer supplyAndGetResult() {
        Flow rt = Flows.currentRuntime();
        Integer res =  rt.supply(() -> 3).get();

        return res;
    }
}

