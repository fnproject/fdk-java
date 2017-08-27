package com.fnproject.fn.testing;

import java.io.InputStream;
import java.util.Map;

interface Invocation {
    /**
     * Simulate the run an external function or continuation step.
     * This method is responsible for completing its embedded Future - which will deliver the
     * response (that is, a full HTTP response) to the function executor.
     */
    void invoke(PlatformSimulator simulator);

    interface PlatformSimulator {
        /**
         * Invoke a single continuation and return the parsed http response
         *
         * @param runtimeEnvironment simulated system environment that the continuation runs holds things like
         *                           {@code $COMPLETER_BASE_URL}
         * @param httpRequest request that contains the serialised java lambda body / closure that form the continuation
         * @return the byte[] body of a successful invocation
         * @throws FunctionError to indicate that something went wrong inside the runtime
         * @throws PlatformError to simulate a failure in the Functions platform itself
         */
        byte[] invokeContinuation(Map<String, String> runtimeEnvironment, InputStream httpRequest) throws FunctionError, PlatformError;

        byte[] invokeExternalFunction(String functionId, byte[] body) throws FunctionError, PlatformError;
    }
}
