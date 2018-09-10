package com.fnproject.fn.api;

/**
 * RuntimeFeatures are classes that configure the Fn Runtime prior to startup and can be loaded by annotating the function class with a {@link FnFeature} annotation
 * Created on 10/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public interface RuntimeFeature {

    /**
     * Initialize the runtime context for this function
     *
     * @param context a runtime context to initalize
     */
    void initialize(RuntimeContext context);
}
