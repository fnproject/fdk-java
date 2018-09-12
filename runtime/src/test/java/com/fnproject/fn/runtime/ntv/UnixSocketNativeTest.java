package com.fnproject.fn.runtime.ntv;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
        System.load("/Users/OCliffe/ws/jfaas/runtime-native/src/main/c/libfnunixsocket.dylib");
    }

    @Test
    public void shouldHandleBasicSocketOperations() throws Exception {


        try {
            UnixSocketNative.bind(-1, "/tmp/foo");
            fail("should have thrown ");
        } catch (UnixSocketException ignored) {
        }

        try {
            UnixSocketNative.close(2345);
            fail("should have thrown ");
        } catch (UnixSocketException ignored) {
        }

        int socket = UnixSocketNative.socket();
        assertThat(socket).isGreaterThan(0);
        File testSock = createSocketFile();
        UnixSocketNative.bind(socket, testSock.getPath());
        UnixSocketNative.close(socket);

        socket = UnixSocketNative.socket();

        try {
            // send to unconnected
            UnixSocketNative.send(socket, new byte[0], 0, 0);
            fail("should have failed");
        } catch (UnixSocketException ignored) {
        }


        int serverSock = 0;
        int clientSock = 0;
        File sockFile = createSocketFile();
        try {
            serverSock = UnixSocketNative.socket();
            UnixSocketNative.listen(serverSock, 1);
            clientSock = UnixSocketNative.accept(serverSock, 0);
            UnixSocketNative.send(socket, new byte[0], 0, 0);
            fail("should have failed");
        } catch (UnixSocketException ignored) {

        } finally {
            if (serverSock > 0) {
                UnixSocketNative.close(serverSock);
            }
            if (clientSock > 0) {
                UnixSocketNative.close(clientSock);
            }
            sockFile.delete();
        }

    }

    File createSocketFile() throws IOException {
        File f = File.createTempFile("socket", "sock");
        f.delete();
        f.deleteOnExit();
        return f;
    }

    @Test
    public void shouldDoEndToEnd() throws Exception {

        CountDownLatch ready = new CountDownLatch(1);

        CompletableFuture<String> result = new CompletableFuture<>();

        File serverSock = createSocketFile();

        Thread t = new Thread(() -> {
            try {
                ready.await();
                System.err.println("client ready");
                int clientSocket = UnixSocketNative.socket();
                UnixSocketNative.connect(clientSocket, serverSock.getPath());
                System.err.println("client connecteds");

                UnixSocketNative.send(clientSocket, "hello world".getBytes(), 0, "hello world".length());
                System.err.println("data sent");


                byte[] buf = new byte[10000];

                int len = UnixSocketNative.recv(clientSocket, buf, 0, buf.length);
                System.err.println("got result");

                String v = new String(buf, 0, len);
                UnixSocketNative.close(clientSocket);
                result.complete(v);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        t.start();

        int serverSocket = UnixSocketNative.socket();
        assertThat(serverSocket).isGreaterThan(0);
        UnixSocketNative.bind(serverSocket, serverSock.getPath());
        UnixSocketNative.listen(serverSocket, 1);

        ready.countDown();
        int cs = UnixSocketNative.accept(serverSocket, 0);
        System.err.println("got server ready" + cs);

        try {// bad offset
            UnixSocketNative.send(cs, new byte[10], -1, 1);
            fail("shoudl reject bad offset");
        } catch (IllegalArgumentException ignored) {
        }

        try {// bad offset
            UnixSocketNative.send(cs, new byte[10], 10, 1);
            fail("shoudl reject bad offset");
        } catch (IllegalArgumentException ignored) {
        }

        try {// bad offset
            UnixSocketNative.send(cs, new byte[10], 0, -1);
            fail("should reject bad length");
        } catch (IllegalArgumentException ignored) {
        }


        byte[] buf = new byte[10000];
        int read = UnixSocketNative.recv(cs, buf, 0, buf.length);

        assertThat(new String(buf, 0, read)).isEqualTo("hello world");

        UnixSocketNative.send(cs, "OK".getBytes(), 0, 2);
        UnixSocketNative.close(cs);


    }
}
