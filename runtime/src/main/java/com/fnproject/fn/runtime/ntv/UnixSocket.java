package com.fnproject.fn.runtime.ntv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This approximates a Java.net.socket for many operations but not by any means all
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public final class UnixSocket extends Socket {
    // Fall back to WTF for most unsupported operations
    private static final SocketImpl fakeSocketImpl = new SocketImpl() {
        @Override
        protected void create(boolean stream) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void connect(String host, int port) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void bind(InetAddress host, int port) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void listen(int backlog) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void accept(SocketImpl s) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        protected InputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int available() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void sendUrgentData(int data) throws IOException {
            throw new UnsupportedOperationException();

        }

        @Override
        public void setOption(int optID, Object value) throws SocketException {
            throw new UnsupportedOperationException();

        }

        @Override
        public Object getOption(int optID) throws SocketException {
            throw new UnsupportedOperationException();
        }
    };


    private final int fd;
    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicBoolean inputClosed = new AtomicBoolean();
    private final AtomicBoolean outputClosed = new AtomicBoolean();

    private final InputStream in;
    private final OutputStream out;


    UnixSocket(int fd) throws SocketException {
        super(fakeSocketImpl);
        this.fd = fd;
        in = new UsInput();
        out = new UsOutput();

    }

    private class UsInput extends InputStream {

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
            if (inputClosed.get()) {
                throw new UnixSocketException("Read on closed stream");
            }

            return UnixSocketNative.recv(fd, b, off, len);
        }

        @Override
        public void close() throws IOException {
            shutdownInput();
        }
    }


    private class UsOutput extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        public void write(byte b[], int off, int len) throws IOException {
            if (outputClosed.get()) {
                throw new UnixSocketException("Write to closed stream");
            }
            Objects.requireNonNull(b);
            while (len > 0) {
                int sent = UnixSocketNative.send(fd, b, off, len);

                if (sent == 0) {
                    throw new UnixSocketException("No data written to buffer");
                }
                off = off + sent;
                len = len - sent;
            }
        }

        @Override
        public void close() throws IOException {
            shutdownOutput();
        }
    }


    public static UnixSocket connect(String destination) throws IOException {
        int fd = UnixSocketNative.socket();
        UnixSocketNative.connect(fd, destination);
        return new UnixSocket(fd);
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }


    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        UnixSocketNative.setRecvBufSize(fd, size);
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        UnixSocketNative.setSendBufSize(fd, size);
    }


    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        UnixSocketNative.setRecvTimeout(fd, timeout);

    }

    @Override
    public int getSoTimeout() throws SocketException {
        return UnixSocketNative.getRecvTimeout(fd);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public InetAddress getLocalAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public int getLocalPort() {
        return -1;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }


    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isBound() {
        return true;
    }


    @Override
    public boolean isClosed() {
        return closed.get();
    }


    @Override
    public boolean isInputShutdown() {
        return inputClosed.get();
    }

    @Override
    public boolean isOutputShutdown() {
        return outputClosed.get();
    }

    @Override
    public void shutdownInput() throws IOException {
        if (inputClosed.compareAndSet(false, true)) {
            UnixSocketNative.shutdown(fd, true, false);
        } else {
            throw new SocketException("Input already shut down");
        }
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (outputClosed.compareAndSet(false, true)) {
            UnixSocketNative.shutdown(fd, false, true);
        } else {
            throw new SocketException("Output already shut down");
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            UnixSocketNative.close(fd);
        }
    }


}
