package com.fnproject.fn.runtime.ntv;

import java.net.SocketException;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixSocketException extends SocketException {
    public UnixSocketException(String message, String detail) {
        super(message + ":" + detail);
    }

    public UnixSocketException(String message) {
        super(message);
    }
}
