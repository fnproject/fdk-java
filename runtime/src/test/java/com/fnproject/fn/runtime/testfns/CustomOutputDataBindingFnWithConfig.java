package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.testfns.coercions.StringReversalCoercion;


public class CustomOutputDataBindingFnWithConfig {
    @FnConfiguration
    public static void outputConfig(RuntimeContext ctx){
        ctx.addOutputCoercion(new StringReversalCoercion());
    }

    public String echo(String s){ return s; }
}
