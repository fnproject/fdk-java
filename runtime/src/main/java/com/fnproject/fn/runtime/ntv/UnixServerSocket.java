package com.fnproject.fn.runtime.ntv;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixServerSocket implements Closeable {
    private final int fd;
    boolean closed;


    private UnixServerSocket(int fd) throws UnixSocketException {
        this.fd = fd;
    }

    public static UnixServerSocket listen(String fileName, int backlog) throws UnixSocketException {
        int fd = UnixSocketNative.socket();

        try {
            UnixSocketNative.bind(fd, fileName);
        } catch (UnixSocketException e) {
            UnixSocketNative.close(fd);
            throw e;
        }


        try {
            UnixSocketNative.listen(fd, backlog);
        } catch (UnixSocketException e) {
            UnixSocketNative.close(fd);
            throw e;
        }
        return new UnixServerSocket(fd);

    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            UnixSocketNative.close(fd);
            closed = true;
        }
    }

    public UnixSocket accept(long timeoutMillis) throws UnixSocketException {
        int newFd = UnixSocketNative.accept(fd, timeoutMillis);
        if (newFd == 0) {
            return null;
        }
        return new UnixSocket(newFd);
    }


}
