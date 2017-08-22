package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.FnConfiguration;

public class CustomDataBindingFnWithNoUserCoersions {

    @FnConfiguration
    public static void inputConfig(RuntimeContext ctx){
    }

    public String echo(String s){
        return s;
    }
}
