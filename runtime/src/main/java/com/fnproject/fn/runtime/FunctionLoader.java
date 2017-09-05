package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;

public interface FunctionLoader {
    MethodWrapper loadClass(String className, String fnName);
}
