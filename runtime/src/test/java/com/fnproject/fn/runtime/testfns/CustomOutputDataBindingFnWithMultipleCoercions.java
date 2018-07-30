package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.testfns.coercions.StringReversalCoercion;
import com.fnproject.fn.runtime.testfns.coercions.StringUpperCaseCoercion;

public class CustomOutputDataBindingFnWithMultipleCoercions {

    @FnConfiguration
    public static void outputConfig(RuntimeContext ctx){
        ctx.addOutputCoercion(new StringUpperCaseCoercion());
        ctx.addOutputCoercion(new StringReversalCoercion());
    }

    public String echo(String s){ return s; }
}
