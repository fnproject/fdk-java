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

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 12/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class UnixServerSocket implements Closeable {
    private final int fd;
    private final AtomicBoolean closed = new AtomicBoolean();

    private UnixServerSocket(int fd)  {
        this.fd = fd;
    }


    public static UnixServerSocket listen(String fileName, int backlog) throws IOException {
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
        if (closed.compareAndSet(false,true)) {
            UnixSocketNative.close(fd);
        }
    }

    public UnixSocket accept(long timeoutMillis) throws IOException {
        if (closed.get()) {
            throw new SocketException("accept on closed socket");
        }
        int newFd = UnixSocketNative.accept(fd, timeoutMillis);
        if (newFd == 0) {
            return null;
        }
        return new UnixSocket(newFd);
    }


}
