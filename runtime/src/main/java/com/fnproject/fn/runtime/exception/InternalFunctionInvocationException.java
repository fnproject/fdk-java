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

package com.fnproject.fn.runtime.exception;

import com.fnproject.fn.api.OutputEvent;

/**
 * The user's code caused an exception - this carries an elided stack trace of the error with respect to only the user's code.
 */
public final class InternalFunctionInvocationException extends RuntimeException {

    private final Throwable cause;
    private final OutputEvent event;

    /**
     * create a function invocation exception
     *
     * @param message       private message for this exception -
     * @param target        the underlying user exception that triggered this failure
     */
    public InternalFunctionInvocationException(String message, Throwable target) {
        super(message);
        this.cause = target;
        this.event = OutputEvent.fromBytes(new byte[0], OutputEvent.Status.FunctionError, null);
    }


    /**
     * create a function invocation exception
     *
     * @param message       private message for this exception -
     * @param target        the underlying user exception that triggered this failure
     * @param event         the output event
     */
    public InternalFunctionInvocationException(String message, Throwable target, OutputEvent event) {
        super(message);
        this.cause = target;
        this.event = event;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    /**
     * map this exception to an output event
     * @return the output event associated with this exception
     */
    public OutputEvent toOutput() {
        return event;
    }

}
