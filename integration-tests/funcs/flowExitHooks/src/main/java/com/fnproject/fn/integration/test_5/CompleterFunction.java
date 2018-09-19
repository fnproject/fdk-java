package com.fnproject.fn.integration.test_5;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@FnFeature(FlowFeature.class)
public class CompleterFunction implements Serializable {

    private final URL callbackURL;

    public CompleterFunction(RuntimeContext rtc) throws Exception {
        callbackURL = URI.create(rtc.getConfigurationByKey("TERMINATION_HOOK_URL").orElseThrow(() -> new RuntimeException("No config set "))).toURL();
    }

    public Integer handleRequest(String input) {
        Flow fl = Flows.currentFlow();
        fl.addTerminationHook((ignored) -> {
            try {
                HttpURLConnection con = (HttpURLConnection) callbackURL.openConnection();

                System.err.println("Ran the hook.");

                con.setRequestMethod("GET");
                if (con.getResponseCode() != 200) {
                    throw new RuntimeException("Got bad code from callback");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        return fl.supply(() -> {
            return 42;
        }).get();
    }

}
