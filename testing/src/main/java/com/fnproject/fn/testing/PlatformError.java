package com.fnproject.fn.testing;

/**
 * An Exception that can be used in invocations of stubbed external functions to signal the failure of the external
 * function due to a simulated infrastructure error in the Oracle Functions platform
 */
public class PlatformError extends Exception {
    public PlatformError() {
    }

    public PlatformError(String s) {
        super(s);
    }
}
