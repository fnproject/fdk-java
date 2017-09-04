package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.OutputEvent;

import java.util.Optional;

public class SpringCloudFunctionInvoker implements FunctionInvoker {
    public final Class<?> configClass;

    public SpringCloudFunctionInvoker(Class<?> configClass) {
        this.configClass = configClass;
    }

    @Override
    public Optional<OutputEvent> tryInvoke(InvocationContext ctx, InputEvent evt) {
        return null;
    }
}
