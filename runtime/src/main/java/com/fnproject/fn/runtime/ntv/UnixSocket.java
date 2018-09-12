package com.fnproject.fn.runtime.ntv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixSocket extends Socket {
    private final int fd;
    private AtomicBoolean closed = new AtomicBoolean();
    private final InputStream in;
    private final OutputStream out;

    private final int maxSendSize = 1024 * 1024;

    UnixSocket(int fd) {
        this.fd = fd;
        in = new UsInput();
        out = new UsOutput();
    }

    private class UsInput extends InputStream {

        boolean closed;
        long counter = 0;

        @Override
        public int read() throws IOException {

            byte[] buf = new byte[1];
            int rv = read(buf, 0, 1);
            if (rv == -1) {
                return -1;
            }
            return (int) buf[0];
        }


        public int read(byte b[]) throws IOException {
            return this.read(b, 0, b.length);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (this.closed) {
                throw new UnixSocketException("Read on closed stream");
            }

            int read = UnixSocketNative.recv(fd, b, off, len);
            counter += read;
            return read;
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
        }
    }


    private class UsOutput extends OutputStream {
        boolean closed;

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        public void write(byte b[], int off, int len) throws IOException {
            if (this.closed) {
                throw new UnixSocketException("Write to closed stream");
            }
            Objects.requireNonNull(b);
            while (len > 0) {
                int chunkSize = Math.min(len, maxSendSize);
                int sent = UnixSocketNative.send(fd, b, off, chunkSize);

                if (sent == 0) {
                    throw new UnixSocketException("No data written to buffer");
                }
                off = off + sent;
                len = len - sent;
            }
        }

        @Override
        public void close() throws IOException {
            this.closed = true;

        }
    }


    public static UnixSocket connect(String destination) throws UnixSocketException {
        int fd = UnixSocketNative.socket();
        UnixSocketNative.connect(fd, destination);
        return new UnixSocket(fd);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false,true)) {
            UnixSocketNative.close(fd);
        }
    }


}
