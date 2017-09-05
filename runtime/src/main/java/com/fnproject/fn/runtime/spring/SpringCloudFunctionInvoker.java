package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.MethodFunctionInvoker;

import java.util.Optional;

public class SpringCloudFunctionInvoker extends MethodFunctionInvoker {

    private final RuntimeContext runtimeContext;
    private final SpringCloudFunctionDiscovery functionDiscovery;

    public SpringCloudFunctionInvoker(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
        this.functionDiscovery = new SpringCloudFunctionDiscovery(runtimeContext);
    }

    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        functionDiscovery.discover();
        functionDiscovery.getFunction();
        // TODO
        // coerce input based on Function type
        // invoke Function with input
        // coerce output based on Function type
        // build output event
        return null;
    }
}
