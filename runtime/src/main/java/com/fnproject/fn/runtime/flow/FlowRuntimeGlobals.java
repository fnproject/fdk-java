package com.fnproject.fn.runtime.flow;

import java.util.Objects;

/**
 * Globals for injecting testing state into flow
 */
public class FlowRuntimeGlobals {
    private static CompleterClientFactory completerClientFactory = null;

    /**
     * Resets the state of the completer client factory to defaults; this is primarily for testing
     */
    public static void resetCompleterClientFactory() {
        completerClientFactory = null;
    }

    /**
     * override the default completer client factory
     *
     * @param currentClientFactory a new client factory to be used to create flows
     */
    public static void setCompleterClientFactory(CompleterClientFactory currentClientFactory) {
        completerClientFactory = Objects.requireNonNull(currentClientFactory);
    }

    /**
     * return the current Fn Flow client factory;
     *
     * @return
     */
    public static CompleterClientFactory getCompleterClientFactory() {
        return completerClientFactory;
    }
}
