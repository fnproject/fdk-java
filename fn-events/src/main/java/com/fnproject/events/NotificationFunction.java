package com.fnproject.events;

import com.fnproject.events.coercion.NotificationCoercion;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public abstract class NotificationFunction<T> {

    @FnConfiguration
    public void configure(RuntimeContext ctx){
        ctx.addInputCoercion(NotificationCoercion.instance());
    }

    public abstract void handler(NotificationMessage<T> notification);
}
