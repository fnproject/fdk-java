package com.fnproject.fn.runtime.ntv;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixSocketNativeTest {

    @BeforeClass
    public static void init() {
        System.setProperty("com.fnproject.java.native.libdir", new File("src/main/c/").getAbsolutePath());
    }

    File createSocketFile() throws IOException {
        File f = File.createTempFile("socket", "sock");
        f.delete();
        f.deleteOnExit();
        return f;
    }

    @Test
    public void shouldHandleBind() throws Exception {


        try { // invalid socket
            UnixSocketNative.bind(-1, createSocketFile().getAbsolutePath());
            fail("should have thrown an invalid argument");
        } catch (UnixSocketException ignored) {
        }

        int socket = UnixSocketNative.socket();
        try { // invalid file location
            UnixSocketNative.bind(socket, "/tmp/foodir/socket");
            fail("should have thrown an invalid argument");
        } catch (UnixSocketException ignored) {
        } finally {
            UnixSocketNative.close(socket);
        }


        socket = UnixSocketNative.socket();
        File socketFile = createSocketFile();
        try { // valid bind
            UnixSocketNative.bind(socket, socketFile.getAbsolutePath());
        } finally {
            UnixSocketNative.close(socket);
        }
    }

    public <T> CompletableFuture<T> runServerLoop(Callable<T> loop) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            try {
                result.complete(loop.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        t.start();
        return result;
    }

    @Test
    public void shouldHandleConnectAccept() throws Exception {

        // invalid socket
        {
            try {
                UnixSocketNative.connect(-1, "/tmp/nonexistant_path.sock");
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            }
        }


        // unknown path
        {
            int socket = UnixSocketNative.socket();
            try {
                UnixSocketNative.connect(socket, "/tmp/nonexistant_path.sock");
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            } finally {
                UnixSocketNative.close(socket);
            }
        }
        // accept rejects <zero timeout
        {
            File serverSocket = createSocketFile();
            int ss = UnixSocketNative.socket();
            long startTime = System.currentTimeMillis();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                int cs = UnixSocketNative.accept(ss, -1);
            } catch (IllegalArgumentException ignored) {
            } finally {
                UnixSocketNative.close(ss);
            }
        }


        // accept honors timeout
        {
            File serverSocket = createSocketFile();
            int ss = UnixSocketNative.socket();
            long startTime = System.currentTimeMillis();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                int cs = UnixSocketNative.accept(ss, 100);
            } catch (UnixSocketException ignored) {
            } finally {
                UnixSocketNative.close(ss);
            }
            assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        }


        // valid connect
        {
            CountDownLatch ready = new CountDownLatch(1);
            File serverSocket = createSocketFile();

            CompletableFuture<Boolean> sresult = runServerLoop(() -> {
                int ss = UnixSocketNative.socket();
                try {
                    UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                    UnixSocketNative.listen(ss, 1);
                    ready.countDown();
                    int cs = UnixSocketNative.accept(ss, 0);
                    return cs > 0;
                } finally {
                    UnixSocketNative.close(ss);
                }

            });
            ready.await();
            int cs;
            cs = UnixSocketNative.socket();
            UnixSocketNative.connect(cs, serverSocket.getAbsolutePath());

            assertThat(sresult.get()).isTrue();
        }

    }

    @Test
    public void shouldHonorWrites() throws Exception {

        CountDownLatch ready = new CountDownLatch(1);
        File serverSocket = createSocketFile();

        CompletableFuture<byte[]> result = runServerLoop(() -> {
            int ss = UnixSocketNative.socket();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                ready.countDown();
                int cs = UnixSocketNative.accept(ss, 0);
                byte[] buf = new byte[100];
                int read = UnixSocketNative.recv(cs, buf, 0, buf.length);
                byte[] newBuf = new byte[read];
                System.arraycopy(buf, 0, newBuf, 0, read);

                return newBuf;
            } finally {
                UnixSocketNative.close(ss);
            }
        });


        {// zero byte write is a noop
            ready.await();
            int cs = UnixSocketNative.socket();
            UnixSocketNative.connect(cs, serverSocket.getAbsolutePath());

            // must NPE  on buff
            try {
                UnixSocketNative.send(cs, null, 0, 10);
                fail("should have NPEd");
            } catch (NullPointerException ignored) {

            }


            // invalid offset
            try {
                UnixSocketNative.send(cs, "hello".getBytes(), 100, 10);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // invalid offset
            try {
                UnixSocketNative.send(cs, "hello".getBytes(), -1, 10);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // invalid offset
            try {
                UnixSocketNative.send(cs, "hello".getBytes(), 0, 10);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            try {
                // Must nop on write
                UnixSocketNative.send(cs, new byte[0], 0, 0);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // validate a real write to be sure
            UnixSocketNative.send(cs, "hello".getBytes(), 0, 5);
            byte[] got = result.get();

            assertThat(got).isEqualTo("hello".getBytes());
        }
    }


    @Test
    public void shouldHonorReads() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        File serverSocket = createSocketFile();
        CompletableFuture<Boolean> result = runServerLoop(() -> {
            int ss = UnixSocketNative.socket();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                ready.countDown();
                int cs = UnixSocketNative.accept(ss, 0);
                int read = UnixSocketNative.send(cs, "hello".getBytes(), 0, 5);
                UnixSocketNative.close(cs);
                return true;
            } finally {
                UnixSocketNative.close(ss);
            }
        });


        {// zero byte write is a noop
            ready.await();
            int cs = UnixSocketNative.socket();
            UnixSocketNative.connect(cs, serverSocket.getAbsolutePath());

            // must NPE  on buff
            try {
                UnixSocketNative.recv(cs, null, 0, 10);
                fail("should have NPEd");
            } catch (NullPointerException ignored) {

            }

            // invalid offset
            try {
                UnixSocketNative.recv(cs, new byte[5], -1, 1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }
            // invalid length
            try {
                UnixSocketNative.recv(cs, new byte[5], 0, -1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // invalid length
            try {
                UnixSocketNative.recv(cs, new byte[5], 0, 0);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // invalid offset beyond buffer
            try {
                UnixSocketNative.recv(cs, new byte[5], 100, 10);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // invalid offset
            try {
                UnixSocketNative.recv(cs, "hello".getBytes(), -1, 10);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {

            }

            // validate a real write to be sure
            byte[] buf = new byte[5];

            int count = UnixSocketNative.recv(cs, buf, 0, 5);
            assertThat(count).isEqualTo(5);

            assertThat(buf).isEqualTo("hello".getBytes());
        }
    }

    @Test
    public void shouldSetSocketOpts() throws Exception {

        int sock = UnixSocketNative.socket();
        try {
            try {
                UnixSocketNative.setSendBufSize(-1, 1);
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            }

            try {
                UnixSocketNative.setSendBufSize(sock, -1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {
            }

            UnixSocketNative.setSendBufSize(sock, 65535);


            try {
                UnixSocketNative.setRecvBufSize(-1, 1);
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            }

            try {
                UnixSocketNative.setRecvBufSize(sock, -1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {
            }

            UnixSocketNative.setRecvBufSize(sock, 65535);


            try {
                UnixSocketNative.setSendTimeout(-1, 1);
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            }

            try {
                UnixSocketNative.setSendTimeout(sock, -1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {
            }

            UnixSocketNative.setSendTimeout(sock, 2000);
            assertThat(UnixSocketNative.getSendTimeout(sock)).isEqualTo(2000);


            try {
                UnixSocketNative.setRecvTimeout(-1, 1);
                fail("should have failed");
            } catch (UnixSocketException ignored) {
            }

            try {
                UnixSocketNative.setRecvTimeout(sock, -1);
                fail("should have IAEd");
            } catch (IllegalArgumentException ignored) {
            }

            UnixSocketNative.setRecvTimeout(sock, 3000);
            assertThat(UnixSocketNative.getRecvTimeout(sock)).isEqualTo(3000);


        } finally {
            UnixSocketNative.close(sock);
        }

    }


    @Test
    public void shouldHandleReadTimeouts() throws Exception {

        CountDownLatch ready = new CountDownLatch(1);
        File serverSocket = createSocketFile();

        CompletableFuture<Boolean> result = runServerLoop(() -> {
            int ss = UnixSocketNative.socket();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                ready.countDown();
                int cs = UnixSocketNative.accept(ss, 0);
                Thread.sleep(100);
                int read = UnixSocketNative.send(cs, "hello".getBytes(), 0, 5);
                UnixSocketNative.close(cs);
                return true;
            } finally {
                UnixSocketNative.close(ss);
            }
        });

        int clientFd = UnixSocketNative.socket();
        UnixSocketNative.setRecvTimeout(clientFd,50);

        ready.await();
        UnixSocketNative.connect(clientFd,serverSocket.getAbsolutePath());
        byte[] buf = new byte[100];
        try {
            UnixSocketNative.recv(clientFd, buf, 0, buf.length);
            fail("should have timed out");
        }catch (SocketTimeoutException ignored){
        }

    }


    @Test
    public void shouldHandleConnectTimeouts() throws Exception {

        CountDownLatch ready = new CountDownLatch(1);
        File serverSocket = createSocketFile();

        CompletableFuture<Boolean> result = runServerLoop(() -> {
            int ss = UnixSocketNative.socket();
            try {
                UnixSocketNative.bind(ss, serverSocket.getAbsolutePath());
                UnixSocketNative.listen(ss, 1);
                ready.countDown();
                Thread.sleep(1000);

                return true;
            } finally {
                UnixSocketNative.close(ss);
            }
        });

        int clientFd = UnixSocketNative.socket();
        UnixSocketNative.setSendTimeout(clientFd,50);
        ready.await();
        try {
            UnixSocketNative.connect(clientFd,serverSocket.getAbsolutePath());

        }catch (SocketTimeoutException ignored){
        }

    }

}
