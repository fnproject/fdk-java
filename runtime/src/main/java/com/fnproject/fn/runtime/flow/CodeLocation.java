package com.fnproject.fn.runtime.flow;

import java.util.Objects;

public class CodeLocation {
    private final String stackTrace;

    private CodeLocation(String location) {
        this.stackTrace = Objects.requireNonNull(location);
    }

    public String getLocation(){
        return stackTrace;
    }

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
