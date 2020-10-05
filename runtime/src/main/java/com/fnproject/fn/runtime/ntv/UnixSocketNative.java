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

package com.fnproject.fn.runtime.ntv;


import java.io.IOException;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
class UnixSocketNative {

    public UnixSocketNative() {}
    
    static {
        String lib = System.mapLibraryName("fnunixsocket");


        String libLocation = System.getProperty("com.fnproject.java.native.libdir");
        if (libLocation != null) {
            if (!libLocation.endsWith("/")) {
                libLocation = libLocation + "/";
            }
            lib = libLocation + lib;
            System.load(lib);
        }else{
            System.loadLibrary("fnunixsocket");
        }
    }

    public static native int socket() throws IOException;

    public static native void bind(int socket, String path) throws UnixSocketException;

    public static native void connect(int socket, String path) throws IOException;

    public static native void listen(int socket, int backlog) throws UnixSocketException;

    public static native int accept(int socket, long timeoutMs) throws IOException;

    public static native int recv(int socket, byte[] buffer, int offset, int length) throws IOException;

    public static native int send(int socket, byte[] buffer, int offset, int length) throws IOException;

    public static native void close(int socket) throws UnixSocketException;

    public static native void setSendTimeout(int socket, int timeout) throws UnixSocketException;

    public static native int getSendTimeout(int socket) throws IOException;

    public static native void setRecvTimeout(int socket, int timeout) throws UnixSocketException;

    public static native int getRecvTimeout(int socket) throws UnixSocketException;

    public static native void setSendBufSize(int socket, int bufSize) throws UnixSocketException;

    public static native void setRecvBufSize(int socket, int bufSize) throws UnixSocketException;

    public static native void shutdown(int socket, boolean input, boolean output) throws UnixSocketException;
}
