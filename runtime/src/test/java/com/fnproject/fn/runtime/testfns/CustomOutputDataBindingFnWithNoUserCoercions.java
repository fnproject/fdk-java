package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.FnConfiguration;

public class CustomOutputDataBindingFnWithNoUserCoercions {

    @FnConfiguration
    public static void outputConfig(RuntimeContext ctx){
    }

    public String echo(String s){ return s; }
}
