package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.OutputBinding;
import com.fnproject.fn.runtime.testfns.coercions.StringReversalCoercion;

public class CustomOutputDataBindingFnWithAnnotation {

    @OutputBinding(coercion=StringReversalCoercion.class)
    public String echo(String s){ return s; }

}
