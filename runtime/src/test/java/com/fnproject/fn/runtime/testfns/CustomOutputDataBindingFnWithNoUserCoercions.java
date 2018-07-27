package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public class CustomOutputDataBindingFnWithNoUserCoercions {

    @FnConfiguration
    public static void outputConfig(RuntimeContext ctx){
    }

    public String echo(String s){ return s; }
}
