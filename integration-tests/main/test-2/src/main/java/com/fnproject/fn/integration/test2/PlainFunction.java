package com.fnproject.fn.integration.test2;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public class PlainFunction {

    private String greeting;

    @FnConfiguration
    public void configuration(RuntimeContext ctx) {
        System.err.println("Configuration called");
        this.greeting = ctx.getConfigurationByKey("GREETING")
                           .orElseThrow(() -> new RuntimeException("Greeting must be set"));
    }

    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world"  : input;

        return greeting + ", " + name + "!";
    }
}
