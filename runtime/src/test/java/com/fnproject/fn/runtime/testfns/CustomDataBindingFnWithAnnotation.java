package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.InputBinding;
import com.fnproject.fn.runtime.testfns.coercions.StringReversalCoercion;

public class CustomDataBindingFnWithAnnotation {

    public String echo(@InputBinding(coercion=StringReversalCoercion.class) String s){
        return s;
    }

}
