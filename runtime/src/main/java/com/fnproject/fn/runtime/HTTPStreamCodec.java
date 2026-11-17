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

package com.fnproject.fn.runtime;


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.runtime.exception.FunctionIOException;
import com.fnproject.fn.runtime.exception.FunctionInitializationException;
import com.fnproject.fn.runtime.http.spi.HttpHeader;
import com.fnproject.fn.runtime.http.spi.HttpServerRequest;
import com.fnproject.fn.runtime.http.spi.HttpServerResponse;
import com.fnproject.fn.runtime.ntv.UnixServerSocket;
import com.fnproject.fn.runtime.ntv.UnixSocket;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

/**
 * Fn HTTP Stream over Unix domain sockets  codec
 * <p>
 * <p>
 * This creates a new unix socket on the address specified by env["FN_LISTENER"] - and accepts requests.
 * <p>
 * This currently only handles exactly one concurrent connection
 * <p>
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public final class HTTPStreamCodec implements EventCodec, Closeable {

    public static final String HTTP_STREAM_FORMAT = "http-stream";
    private static final String FN_LISTENER = "FN_LISTENER";
    private final Map<String, String> env;
    private final String fdkVersion;
    private final String runtimeVersion;
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final File socketFile;
    private final CompletableFuture<Boolean> stopped = new CompletableFuture<>();
    private final UnixServerSocket socket;
    private final File tempFile;


    private String randomString() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }


    /**
     * Construct a new HTTPStreamCodec based on the environment
     *
     * @param env        an env map
     * @param fdkVersion the version to report to the runtime
     * @param runtimeVersion underlying JVM version to report to the runtime
     */
    HTTPStreamCodec(Map<String, String> env, String fdkVersion, String runtimeVersion) {
        this.env = Objects.requireNonNull(env, "env");
        this.fdkVersion = Objects.requireNonNull(fdkVersion, "fdkVersion");
        this.runtimeVersion = Objects.requireNonNull(runtimeVersion, "runtimeVersion");
        String listenerAddress = getRequiredEnv(FN_LISTENER);

        if (!listenerAddress.startsWith("unix:/")) {
            throw new FunctionInitializationException("Invalid listener address - it should start with unix:/ :'" + listenerAddress + "'");
        }
        String listenerFile = listenerAddress.substring("unix:".length());

        socketFile = new File(listenerFile);


        UnixServerSocket serverSocket = null;
        File listenerDir = socketFile.getParentFile();
        tempFile = new File(listenerDir, randomString() + ".sock");
        try {

            serverSocket = UnixServerSocket.listen(tempFile.getAbsolutePath(), 1);
            // Adjust socket permissions and move file
            Files.setPosixFilePermissions(tempFile.toPath(), PosixFilePermissions.fromString("rw-rw-rw-"));
            Files.createSymbolicLink(socketFile.toPath(), tempFile.toPath().getFileName());

            this.socket = serverSocket;
        } catch (IOException e) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }

            }
            throw new FunctionInitializationException("Unable to bind to unix socket in " + socketFile, e);
        }


    }


    @Override
    public void runCodec(Handler h) {
        FnHttpHandler handler = new FnHttpHandler(h, fdkVersion, runtimeVersion);
        UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
        mapper.register("*", (request, response, context) ->
            handler.handle(adaptRequest(request), adaptResponse(response)));

        ImmutableHttpProcessor requestProcess = new ImmutableHttpProcessor(new HttpRequestInterceptor[0], new HttpResponseInterceptor[0]);
        HttpService svc = new HttpService(requestProcess, mapper);

        try {

            while (!stopping.get()) {
                try (UnixSocket sock = socket.accept(100)) {
                    if (sock == null) {
                        // timeout during accept, try again
                        continue;
                    }
                    // TODO tweak these properly
                    sock.setSendBufferSize(65535);
                    sock.setReceiveBufferSize(65535);


                    if (stopping.get()) {
                        // ignore IO errors on stop
                        return;
                    }
                    try {
                        DefaultBHttpServerConnection con = new DefaultBHttpServerConnection(65535);
                        con.bind(sock);
                        while (!sock.isClosed()) {
                            try {
                                svc.handleRequest(con, new BasicHttpContext());
                            } catch (HttpException e) {
                                sock.close();
                                throw e;
                            }
                        }
                    } catch (HttpException | IOException e) {
                        System.err.println("FDK Got Exception while handling HTTP request" + e.getMessage());
                        e.printStackTrace();
                        // we continue here and leave the container hot
                    }
                } catch (IOException e) {
                    if (stopping.get()) {
                        // ignore IO errors on stop
                        return;
                    }
                    throw new FunctionIOException("failed to accept connection from platform, terminating", e);
                }

            }
        } finally {
            stopped.complete(true);
        }


    }


    private String getRequiredEnv(String name) {
        String val = env.get(name);
        if (val == null) {
            throw new FunctionInputHandlingException("Required environment variable " + name + " is not set - are you running a function outside of fn run?");
        }
        return val;
    }

    private HttpServerRequest adaptRequest(HttpRequest request) {
        List<HttpHeader> headers = new ArrayList<>();
        for (Header header : request.getAllHeaders()) {
            headers.add(new HttpHeader(header.getName(), header.getValue()));
        }
        List<HttpHeader> snapshot = Collections.unmodifiableList(headers);
        return new HttpServerRequest() {
            @Override
            public String getMethod() {
                return request.getRequestLine().getMethod();
            }

            @Override
            public String getRequestTarget() {
                return request.getRequestLine().getUri();
            }

            @Override
            public List<HttpHeader> getHeaders() {
                return snapshot;
            }

            @Override
            public InputStream getBody() throws IOException {
                if (request instanceof HttpEntityEnclosingRequest) {
                    return ((HttpEntityEnclosingRequest) request).getEntity().getContent();
                }
                return EmptyInputStream.INSTANCE;
            }
        };
    }

    private HttpServerResponse adaptResponse(HttpResponse response) {
        return new HttpServerResponse() {
            @Override
            public void setStatus(int statusCode, String reasonPhrase) {
                response.setStatusLine(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, reasonPhrase));
            }

            @Override
            public void addHeader(String name, String value) {
                response.addHeader(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                response.setHeader(name, value);
            }

            @Override
            public void setBody(byte[] body, String contentType) {
                response.setEntity(new ByteArrayEntity(body, ContentType.parse(contentType)));
            }
        };
    }


    @Override
    public void close() throws IOException {
        if (stopping.compareAndSet(false, true)) {
            socket.close();

            try {
                stopped.get();
            } catch (Exception ignored) {
            }
            socketFile.delete();
            tempFile.delete();
        }

    }
}
