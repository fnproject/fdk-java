package com.fnproject.fn.integration.test_1;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        Flow fl = Flows.currentFlow();

        return fl.supply(() -> Integer.parseInt(input))
                .thenApply((i) -> i + 3)
                .get();
    }
}
