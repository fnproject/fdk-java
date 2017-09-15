package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

import java.io.Serializable;

public class FnFlowsFunction implements Serializable {

    public static void usingFlows() {
        Flows.currentFlow();
    }

    public static void notUsingFlows() {
    }

    public static void supply() {
        Flow fl = Flows.currentFlow();
        fl.supply(() -> 3);
    }

    public static void accessRuntimeMultipleTimes() {
        Flows.currentFlow();
        Flows.currentFlow();
    }

    public static Integer supplyAndGetResult() {
        Flow fl = Flows.currentFlow();
        Integer res =  fl.supply(() -> 3).get();

        return res;
    }
}

