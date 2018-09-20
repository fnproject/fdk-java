package com.example.fn;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

public class TriggerFunction {

    public String handleRequest(String input, HTTPGatewayContext hctx, InvocationContext ctx) {
        String name = (input == null || input.isEmpty()) ? "world" : input;


        ctx.setResponseHeader("MyRawHeader", "bar");
        ctx.addResponseHeader("MyRawHeader", "bob");


        hctx.setResponseHeader("Content-Type", "text/plain");
        hctx.addResponseHeader("MyHTTPHeader", "foo");

        hctx.addResponseHeader("GotMethod", hctx.getMethod());
        hctx.addResponseHeader("GotURL", hctx.getRequestURL());
        hctx.addResponseHeader("GotHeader", hctx.getHeaders().get("Foo").orElse("nope"));

        hctx.setStatusCode(202);
        return "Hello, " + name + "!";
    }

}