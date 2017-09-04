package com.fnproject.fn.runtime.cloudthreads;

import java.util.Objects;

/**
 * Created on 04/09/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class CloudThreadRuntimeGlobals {
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
     * @param currentClientFactory a new client factory to be used to create threads
     */
    public static void setCompleterClientFactory(CompleterClientFactory currentClientFactory) {
        completerClientFactory = Objects.requireNonNull(currentClientFactory);
    }

    public static CompleterClientFactory getCompleterClientFactory() {
        return completerClientFactory;
    }
}
