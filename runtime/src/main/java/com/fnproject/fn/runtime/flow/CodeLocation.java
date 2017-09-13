package com.fnproject.fn.runtime.flow;

import java.util.Objects;

/**
 * Class used to return the code location of the caller
 */
public class CodeLocation {
    private final String stackTrace;

    private CodeLocation(String location) {
        this.stackTrace = Objects.requireNonNull(location);
    }

    public String getLocation(){
        return stackTrace;
    }

    /**
     * It is required that the return is the 'stack[steps + 2]
     * as ths stack has the zeroth element of the form "java.lang.Thread.getStackTrace(Thread.java:1559)"
     * and a first element of the form "com.fnproject.fn.runtime.flow.CodeLocation.fromCallerLocation(CodeLocation.java:28)"
     *
     * @param steps Number of steps from the call of the function
     * @return the code location for the call
     */
    public static CodeLocation fromCallerLocation(int steps){
        if(steps < 0){
            throw new IllegalArgumentException("steps must be positive");
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if(stack.length < steps + 3){
            return null;
        } else {
            StackTraceElement elem = stack[steps + 2];
            return new CodeLocation(elem.toString());
        }
    }
}
