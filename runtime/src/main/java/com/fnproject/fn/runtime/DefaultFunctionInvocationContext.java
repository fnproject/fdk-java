package com.fnproject.fn.runtime;

import java.util.List;
import java.util.Map;
import com.fnproject.fn.api.InputEvent;

public class DefaultFunctionInvocationContext extends FunctionInvocationContext {
    public DefaultFunctionInvocationContext(FunctionRuntimeContext ctx, InputEvent event) {
        super(ctx, event);
    }

    public Map<String, List<String>> getAdditionalResponseHeaders() {
        return super.getAdditionalResponseHeaders();
    }
}
