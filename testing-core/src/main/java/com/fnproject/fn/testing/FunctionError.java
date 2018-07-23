package com.fnproject.fn.testing;

/**
 * An Exception that can be used in invocations of stubbed external functions to signal the failure of the external
 * function due to a simulated error case of the function itself
 */
public class FunctionError extends Exception {
    public FunctionError() {
    }

    public FunctionError(String s) {
        super(s);
    }
}
