package com.fnproject.events;

import com.fnproject.events.coercion.ConnectorHubCoercion;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public abstract class ConnectorHubFunction<T> {

    @FnConfiguration
    public void configure(RuntimeContext ctx){
        ctx.addInputCoercion(ConnectorHubCoercion.instance());
    }

    public abstract void handler(ConnectorHubBatch<T> connectorHubBatch);
}
