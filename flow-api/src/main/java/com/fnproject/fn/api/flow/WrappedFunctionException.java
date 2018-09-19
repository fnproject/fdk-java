package com.fnproject.fn.api.flow;

import java.io.Serializable;

/**
 * Wrapped exception where the root cause of a function failure was not serializable.
 *
 * This exposes the type of the original exception via {@link #getOriginalExceptionType()} and preserves the original exception stacktrace.
 *
 */
public final class WrappedFunctionException extends RuntimeException implements Serializable {
    private final Class<?> originalExceptionType;

    public WrappedFunctionException(Throwable cause){
        super(cause.getMessage());
        this.setStackTrace(cause.getStackTrace());
        this.originalExceptionType = cause.getClass();
    }

    /**
     * Exposes the type of the original error
     * @return the class of the opriginal exception type;
     */
    public Class<?> getOriginalExceptionType() {
        return originalExceptionType;
    }
}
