package com.fnproject.fn.runtime;


import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionConfigurationException;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.unixsocket.UnixSocketConnector;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

/**
 * Fn HTTP codec
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class HTTPStreamCodec implements EventCodec,Closeable {

    private static final String FN_LISTENER="FN_LISTENER";

    private final Map<String, String> env;
    private final Object lock = new Object();
    private final Server s;
    private final UnixSocketConnector sc;

    private Request baseRequest;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public HTTPStreamCodec(Map<String, String> env) throws Exception {
        this.env = env;

        String listener  = env.get(FN_LISTENER);
        if (listener == null) {
            throw new IllegalStateException("Invalid Environment - no FN_LISTENER socket path was specified in the environment");
        }

        Properties loggerProperties = new Properties();
        loggerProperties.put("LEVEL", StdErrLog.LEVEL_OFF);
        Log.setLog(new StdErrLog("/dev/null", loggerProperties));

        s = new Server();
        sc = new UnixSocketConnector(s);
        sc.setUnixSocket(listener);
        s.addConnector(sc);
        s.setHandler(new AbstractHandler() {
            @Override
            public void handle(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response
            ) throws IOException, ServletException {
                synchronized (HTTPStreamCodec.this.lock) {
                    if (HTTPStreamCodec.this.baseRequest != null) {
                        throw new IllegalStateException("Concurrent function invocation not supported");
                    }
                    HTTPStreamCodec.this.baseRequest = baseRequest;
                    HTTPStreamCodec.this.request = request;
                    HTTPStreamCodec.this.response = response;
                    HTTPStreamCodec.this.lock.notify();
                }
                try {
                    synchronized (HTTPStreamCodec.this.lock) {
                        lock.wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });
        s.start();
    }

    private String getRequiredEnv(String name) {
        String val = env.get(name);
        if (val == null) {
            throw new FunctionInputHandlingException("Required environment variable " + name + " is not set - are you running a function outside of fn run?");
        }
        return val;
    }

    @Override
    public Optional<InputEvent> readEvent() {

        try {
            synchronized (lock) {
                lock.wait();
            }

            InputStream bodyStream;
            try {
                bodyStream = request.getInputStream();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return Optional.empty();
            }

            String appName = getRequiredEnv("FN_APP_NAME");
            String route = getRequiredEnv("FN_PATH");
            String method = request.getHeader("X-Fn-Method");
            // TODO remove HTTP-specific stuff
            if (method == null) {
                method = "GET";
            }

            String requestUrl = request.getHeader("X-Fn-Request-URL");
            if (requestUrl == null) {
                requestUrl = "";
            }

            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }

            ReadOnceInputEvent inputEvent = new ReadOnceInputEvent(appName, route, requestUrl, method, bodyStream, com.fnproject.fn.api.Headers.fromMap(headers), QueryParametersParser.getParams(requestUrl));
            return Optional.of(inputEvent);
        } catch (InterruptedException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public void writeEvent(OutputEvent evt) {
        synchronized(lock) {
            evt.getHeaders().getAll().forEach((k, v) -> response.setHeader(k, v));
            evt.getContentType().ifPresent(v -> response.setHeader("Content-Type", v));

            try {
                OutputStream out = response.getOutputStream();
                evt.writeToOutput(out);
            } catch (IOException ignored) {
            }

            this.baseRequest.setHandled(true);

            this.baseRequest = null;
            this.request = null;
            this.response = null;

            lock.notify();
        }
    }

    public void close() {
        try {
            s.stop();
            s.join();
        } catch (Exception ignored) {
        }
    }
}
