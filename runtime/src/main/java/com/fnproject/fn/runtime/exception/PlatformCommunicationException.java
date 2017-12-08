package com.fnproject.fn.runtime.exception;

public class PlatformCommunicationException extends RuntimeException {
    public PlatformCommunicationException(String message) {
        super(message);
    }

    public PlatformCommunicationException(String message, Exception e) {
        super(message, e);
    }
}
