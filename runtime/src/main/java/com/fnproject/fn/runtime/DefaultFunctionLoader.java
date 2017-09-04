package com.fnproject.fn.runtime;

public class DefaultFunctionLoader implements FunctionLoader {
    public FnFunction loadClass(String className, String fnName) {
        return new FnFunction(className, fnName);
    }
}
