package com.fnproject.fn.testing;

import java.io.PrintStream;

/**
 * Created on 07/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface FnTestingRuleFeature {

    /**
     * Prepares a test
     * @param functionClassLoader
     * @param stderr
     * @param cls
     * @param method
     */
    void prepareTest(ClassLoader functionClassLoader, PrintStream stderr, String cls, String method);


    void prepareFunctionClassLoader(FnTestingClassLoader cl);


    void afterTestComplete();
}
