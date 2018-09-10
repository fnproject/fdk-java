package com.fnproject.springframework.function;

import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.RuntimeFeature;

/**
 *
 * The SpringCloudFunctionFeature enables a function to be run with a spring cloud function configuration
 *
 * Created on 10/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class SpringCloudFunctionFeature implements RuntimeFeature {

    @Override
    public void initialize(RuntimeContext ctx) {
        ctx.addInvoker(new SpringCloudFunctionInvoker(ctx.getMethod().getTargetClass()),FunctionInvoker.Phase.Call);
    }
}
