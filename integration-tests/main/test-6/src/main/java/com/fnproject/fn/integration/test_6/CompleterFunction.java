package com.fnproject.fn.integration.test_6;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompleterFunction {

    public Integer handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        try {
            return fl.supply(() -> {
                Thread.sleep(10000);
                return 42;
            }).get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            System.err.println("Caught timeout");
            return 20;
        }
    }

}
