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
