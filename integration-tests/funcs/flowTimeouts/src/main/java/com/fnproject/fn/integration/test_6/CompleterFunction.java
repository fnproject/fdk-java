package com.fnproject.fn.integration.test_6;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@FnFeature(FlowFeature.class)
public class CompleterFunction {

    public String handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        try {
            return fl.supply(() -> {
                Thread.sleep(10000);
                return "nope";
            }).get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            return "timeout";
        }
    }

}
