/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.runtime.flow;


import java.util.Objects;

/**
 * Class used to return the code location of the caller
 */
public class CodeLocation {
    private  static CodeLocation UNKNOWN_LOCATION= new CodeLocation("unknown location");
    private final String location;

    private CodeLocation(String location) {
        this.location = Objects.requireNonNull(location);
    }

    public String getLocation(){
        return location;
    }

    /**
     * Create a code location based on an offset of the stack of the current caller
     *
     * A stack offset of zero represents the calling method and one represents the caller of the calling method
     * If no location information is available then an {@link #unknownLocation()} is returned
     *
     * @param stackOffset depth into the stack relative to the calling method to get the location
     * @return the code location for the call
     */
    public static CodeLocation fromCallerLocation(int stackOffset){
        if(stackOffset < 0){
            throw new IllegalArgumentException("stackOffset must be positive");
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if(stack.length < stackOffset + 3){
            return UNKNOWN_LOCATION;
        } else {
            StackTraceElement elem = stack[stackOffset + 2];
            return new CodeLocation(elem.toString());
        }
    }

    /**
     * Creates a code location representing an unknown source location
     * @return a code location
     */
    public static CodeLocation unknownLocation(){
        return UNKNOWN_LOCATION;
    }
}
