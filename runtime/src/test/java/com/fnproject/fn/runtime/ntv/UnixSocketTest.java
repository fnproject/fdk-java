package com.fnproject.fn.runtime.ntv;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixSocketTest {

    @BeforeClass
    public static void setup() {
        System.setProperty("com.fnproject.java.native.libdir", new File("src/main/c/").getAbsolutePath());
    }

    File createSocketFile() throws IOException {
        File f = File.createTempFile("socket", "sock");
        f.delete();
        f.deleteOnExit();
        return f;
    }

    public byte[] roundTripViaEcho(byte[] data) throws Exception {

        File f = createSocketFile();
        try (UnixServerSocket ss = UnixServerSocket.listen(f.getPath(), 1)) {

            CompletableFuture<byte[]> result = new CompletableFuture<>();
            CountDownLatch cdl = new CountDownLatch(1);
            Thread client = new Thread(() -> {
                try {
                    cdl.await();
                    try (UnixSocket us = UnixSocket.connect(f.getPath())) {
                        byte[] buf = new byte[data.length];
                        us.outputStream().write(data);
                        System.err.println("Wrote "  + data.length + " bytes");
                        DataInputStream dis = new DataInputStream(us.inputStream());
                        dis.readFully(buf);
                        result.complete(buf);
                    }
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
            client.start();

            cdl.countDown();
            UnixSocket in = ss.accept(1000);
            byte[] sbuf = new byte[data.length];
            new DataInputStream(in.inputStream()).readFully(sbuf);
            in.outputStream().write(sbuf);
            in.close();
            return result.get();
        }
    }

    @Test
    public void shouldHandleEmptyData() throws Exception {
        byte[] data = "hello".getBytes();
        Assertions.assertThat(roundTripViaEcho(data)).isEqualTo(data);

    }


    @Test
    public void shouldHandleBigData() throws Exception {
        Random r = new Random();
        byte[] data = new byte[1024 * 1024 * 10];
        r.nextBytes(data);

        Assertions.assertThat(roundTripViaEcho(data)).isEqualTo(data);

    }
}
