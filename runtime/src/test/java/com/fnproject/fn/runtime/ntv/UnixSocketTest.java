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
                        us.setReceiveBufferSize(65535);
                        us.setSendBufferSize(65535);
                        byte[] buf = new byte[data.length];
                        us.getOutputStream().write(data);
                        DataInputStream dis = new DataInputStream(us.getInputStream());
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
            in.setReceiveBufferSize(65535);
            in.setSendBufferSize(65535);
            new DataInputStream(in.getInputStream()).readFully(sbuf);
            in.getOutputStream().write(sbuf);
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
        byte[] dataPart = new byte[2048];

        r.nextBytes(dataPart);

        byte[] data = new byte[1024 * 1024 * 10];
        for (int i = 0 ; i < data.length ;i += dataPart.length){
            System.arraycopy(dataPart,0,data,i,dataPart.length);
        }

        Assertions.assertThat(roundTripViaEcho(data)).isEqualTo(data);

    }
}
