package com.fnproject.fn.testing.flow;

import com.fnproject.fn.testing.FunctionError;
import com.fnproject.fn.testing.PlatformError;

/**
 * A builder for constructing stub external functions
 */
public interface FnFunctionStubBuilder<T> {
    /**
     * Consume the builder and stub the function to return the provided byte array
     *
     * @param result A byte array returned by the function
     * @return The original testing rule (usually {@link FlowTesting}. The builder is consumed.
     */
    T withResult(byte[] result);

    /**
     * Consume the builder and stub the function to throw an error when it is invoked: this simulates a failure of the
     * called function, e.g. if the external function threw an exception.
     *
     * @return The original testing rule (usually {@link FlowTesting}. The builder is consumed.
     */
    T withFunctionError();

    /**
     * Consume the builder and stub the function to throw a platform error, this simulates a failure of the Fn Flow
     * completions platform, and not any error of the user code.
     *
     * @return The original testing rule (usually {@link FlowTesting}. The builder is consumed.
     */
    T withPlatformError();

    /**
     * Consume the builder and stub the function to perform some action; the action is an implementation of the
     * functional interface {@link ExternalFunctionAction}, this gives finer grained control over the behaviour of the
     * stub compared to {@link #withResult(byte[])}, {@link #withFunctionError()} and {@link #withPlatformError()}.
     * <p>
     * Note that there are no thread-safety guarantees on any external state modified in the provided action. If shared
     * external state is accessed, a synchronization mechanism should be used.
     *
     * @param f an action to apply when this function is invoked
     * @return The original testing rule (usually {@link FlowTesting}. The builder is consumed.
     */
    T withAction(ExternalFunctionAction f);

    /**
     * Represents the calling interface of an external function. It takes a byte[] as input,
     * and produces a byte[] as output possibly throwing a {@link FunctionError} to simulate user code failure,
     * or a {@link PlatformError} to simulate a failure of the Fn Flow completions platform.
     */
    interface ExternalFunctionAction {
        /**
         * Run the external function action
         *
         * @param arg the body of the function invocation as a byte array
         * @return the response to the function invocation as a byte array
         * @throws FunctionError if the action should simulate a function failure
         * @throws PlatformError if the action should simulate a platform failure
         */
        byte[] apply(byte[] arg) throws FunctionError, PlatformError;
    }
}
