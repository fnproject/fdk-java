package com.fnproject.fn.integration.test_5;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        Flow rt = Flows.currentRuntime();
        rt.addTerminationHook( (ignored) -> { System.err.println("Ran the hook."); });
        return rt.supply(() -> { Thread.sleep(1000); return 42; }).get();
    }

}
