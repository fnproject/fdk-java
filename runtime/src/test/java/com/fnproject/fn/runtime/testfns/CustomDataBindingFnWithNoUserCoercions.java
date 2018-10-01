package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

public class CustomDataBindingFnWithNoUserCoercions {

    @FnConfiguration
    public static void inputConfig(RuntimeContext ctx){
    }

    public String echo(String s){
        return s;
    }
}
