package com.fnproject.fn.integration.test_5;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

@FnFeature(FlowFeature.class)
public class CompleterFunction {

    public Integer handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        fl.addTerminationHook( (ignored) -> { System.err.println("Ran the hook."); });
        return fl.supply(() -> { Thread.sleep(1000); return 42; }).get();
    }

}
