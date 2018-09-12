package com.fnproject.fn.runtime.ntv;

import java.util.Locale;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
class UnixSocketNative {

    static {
        String lib = System.mapLibraryName("fnunixsocket");


        String libLocation = System.getProperty("com.fnproject.java.native.libdir");
        if (libLocation != null) {
            if (!libLocation.endsWith("/")) {
                libLocation = libLocation + "/";
            }
            lib = libLocation + lib;
        }
        System.load(lib);
    }

    public static native int socket() throws UnixSocketException;

    public static native void bind(int socket, String path) throws UnixSocketException;

    public static native void connect(int socket, String path) throws UnixSocketException;

    public static native void listen(int socket, int backlog) throws UnixSocketException;

    public static native int accept(int socket, long timeoutMs) throws UnixSocketException;

    public static native int recv(int socket, byte[] buffer, int offset, int length) throws UnixSocketException;

    public static native int send(int socket, byte[] buffer, int offset, int length) throws UnixSocketException;

    public static native void close(int socket) throws UnixSocketException;


}
