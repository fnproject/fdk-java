package com.fnproject.fn.runtime;


import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;
import java.util.*;

/**
 * Function testing harness - this provides the call-side of iron functions' process contract for both HTTP and default type functions
 */
public class FnTestHarness implements TestRule {
    private Map<String,String> vars = new HashMap<>();
    private boolean hasEvents = false;
    private InputStream pendingInput = new ByteArrayInputStream(new byte[0]);
    private ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private int exitStatus = -1;

    private final Map<String, String> config = new HashMap<>();

    /**
     * Add a config variable to the function
     * <p>
     * Config names will be translated to upper case with hyphens and spaces translated to _
     * <p>
     * clashing config keys will be overwritten
     *
     * @param key   the configuration key
     * @param value the configuration value
     */
    public void setConfig(String key, String value) {
        config.put(key.toUpperCase().replaceAll("[- ]", "_"), value);
    }

    /**
     * Gets a function config variable by key, or null if absent
     *
     * @param key   the configuration key
     */
    public String getConfig(String key) {
        return config.get(key.toUpperCase().replaceAll("[- ]", "_"));
    }

    /**
     * Builds a mocked input event into the function runtime
     */
    public abstract class EventBuilder {
        protected String method = "GET";
        protected String appName = "appName";
        protected String route = "/route";
        protected String requestUrl = "http://example.com/r/appName/route";
        protected InputStream body = new ByteArrayInputStream(new byte[0]);
        protected int contentLength = 0;
        protected String contentType = null;

        protected Map<String, String> headers = new HashMap<>();

        /**
         * Add a header to the input
         * Duplicate headers will be overwritten
         *
         * @param key   header key
         * @param value header value
         */
        public EventBuilder withHeader(String key, String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            headers.put(key, value);
            return this;
        }

        /**
         * Add a series of headers to the input
         * This may override duplicate headers
         * @param headers  Map of headers to add
         */
        public EventBuilder withHeaders(Map<String, String> headers) {
            headers.forEach(this::withHeader);
            return this;
        }

        /**
         * Set the body of the request by providing an InputStream
         *
         * @param body the bytes of the body
         * @param contentLength how long the body is supposed to be
         */
        public EventBuilder withBody(InputStream body, int contentLength) {
            Objects.requireNonNull(body, "body");
            if (contentLength < 0) {
                throw new IllegalArgumentException("Invalid contentLength");
            }
            // This is for safety. Because we concatenate events, an input stream shorter than content length will cause
            // the implementation to continue reading through to the next http request. We need to avoid a sort of
            // buffer overrun.
            // FIXME: Make InputStream handling simpler.
            SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
            sib.bind(body);
            this.body = new ContentLengthInputStream(sib, contentLength);
            this.contentLength = contentLength;
            return this;
        }

        /**
         * Set the body of the request as a byte array
         *
         * @param body the bytes of the body
         */
        public EventBuilder withBody(byte[] body) {
            Objects.requireNonNull(body, "body");
            return withBody(new ByteArrayInputStream(body), body.length);
        }

        /**
         * Set the body of the request as a string
         *
         * @param body the bytes of the body
         */
        public EventBuilder withBody(String body) {
            byte stringAsBytes[] = Objects.requireNonNull(body, "body").getBytes();
            return withBody(stringAsBytes);
        }

        /**
         * Set the body of the request from a stream
         * @param contentStream  the content of the body
         */
        public EventBuilder withBody(InputStream contentStream) throws IOException {
            return withBody(IOUtils.toByteArray(contentStream));
        }

        /**
         * Set the fn route associated with the call
         *
         * @param route the route
         */
        public EventBuilder withRoute(String route) {
            Objects.requireNonNull(route, "route");
            this.route = route;
            return this;
        }

        /**
         * Set the HTTP method of the incoming request
         *
         * @param method an HTTP method
         * @return
         */
        public EventBuilder withMethod(String method) {
            Objects.requireNonNull(method, "method");
            this.method = method.toUpperCase();
            return this;
        }

        /**
         * Set the app name the incoming event
         *
         * @param appName the app name
         * @return
         */
        public EventBuilder withAppName(String appName) {
            Objects.requireNonNull(appName, "appName");
            this.appName = appName;
            return this;
        }

        /**
         * Set the request URL of the incoming event
         *
         * @param requestUrl the request URL
         * @return
         */
        public EventBuilder withRequestUrl(String requestUrl) {
            Objects.requireNonNull(requestUrl, "requestUrl");
            this.requestUrl = requestUrl;
            return this;
        }

        /**
         * Prepare an event for the configured codec - this sets appropriate environment variable in the Env mock and StdIn mocks.
         * <p>
         *
         * @throws IllegalStateException If the the codec only supports one event and an event has already been enqueued.
         */
        public abstract void enqueue();

        Map<String, String> commonEnv() {
            Map<String, String> env = new HashMap<>();
            env.putAll(config);

            env.put("FN_METHOD", method);
            env.put("FN_APP_NAME", appName);
            env.put("FN_PATH", route);
            return env;
        }
    }

    private final class HttpEventBuilder extends EventBuilder {
        @Override
        public void enqueue() {
            StringBuilder inputString = new StringBuilder();
            // Only set env for first event.
            if (!hasEvents) {
                vars.putAll(commonEnv());
                vars.put("FN_FORMAT", "http");
            }
            inputString.append(method);
            inputString.append(" / HTTP/1.1\r\n");
            inputString.append("Fn_App_name: ").append(appName).append("\r\n");
            inputString.append("Fn_Method: ").append(method).append("\r\n");
            inputString.append("Fn_Path: ").append(route).append("\r\n");
            inputString.append("Fn_Request_Url: ").append(requestUrl).append("\r\n");
            inputString.append("Fn_Call_Id: ").append("call-id").append("\r\n");

            if (contentType != null) {
                inputString.append("Content-Type: ").append(contentType).append("\r\n");
            }

            inputString.append("Content-length: ").append(Integer.toString(contentLength)).append("\r\n");
            headers.forEach((k, v) -> {
                inputString.append(k).append(": ").append(v).append("\r\n");
            });

            // added to the http request as headers to mimic the behaviour of `functions` but should NOT be used as config
            config.forEach((k, v) -> {
                inputString.append(k).append(": ").append(v).append("\r\n");
            });
            inputString.append("\r\n");

            pendingInput = new SequenceInputStream(pendingInput, new ByteArrayInputStream(inputString.toString().getBytes()));
            pendingInput = new SequenceInputStream(pendingInput, body);
            hasEvents = true;
        }
    }

    /**
     * Runs the function runtime with the specified class and method
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(Class<?> cls, String method) {
        thenRun(cls.getName(), method);
    }

    /**
     * Runs the function runtime with the specified  class and method
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(String cls, String method) {
        InputStream oldSystemIn = System.in;
        PrintStream oldSystemOut = System.out;
        PrintStream oldSystemErr = System.err;
        try {
            PrintStream functionOut = new PrintStream(stdOut);
            PrintStream functionErr = new PrintStream(new TeeOutputStream(stdErr, oldSystemErr));
            System.setOut(functionErr);
            System.setErr(functionErr);
            exitStatus = new EntryPoint().run(
                    vars,
                    pendingInput,
                    functionOut,
                    functionErr,
                    cls + "::" + method);
            stdOut.flush();
            stdErr.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.out.flush();
            System.err.flush();
            System.setIn(oldSystemIn);
            System.setOut(oldSystemOut);
            System.setErr(oldSystemErr);
        }
    }


    /**
     * Get the exit status of the runtime;
     *
     * @return the system exit status
     */
    public int exitStatus() {
        return exitStatus;
    }

    /**
     * mock an HTTP event (Input and stdOut encoded as HTTP frames)
     *
     * @return a new event builder.
     */
    public EventBuilder givenEvent() {
        return new HttpEventBuilder();
    }


    /**
     * Get the stderr stream returned by the function as a byte array
     *
     * @return the stderr stream as bytes from the runtime
     */
    public byte[] getStdErr() {
        return stdErr.toByteArray();
    }


    /**
     * Gets the stderr stream returned by the function as a String
     *
     * @return the stderr stream as a string from the function
     */
    public String getStdErrAsString() {
        return new String(stdErr.toByteArray());
    }


    /**
     * Get the output produced by the runtime as a byte array
     *
     * @return the bytes returned by the function runtime;
     */
    public byte[] getStdOut() {
        return stdOut.toByteArray();
    }

    /**
     * Get the output produced by the runtime as a string.
     * <p>
     * For Hot functions this will include the HTTP envelope  with (possibly) multiple messages
     *
     * @return a string representation of the function output
     */
    public String getStdOutAsString() {
        return new String(stdOut.toByteArray());
    }


    /**
     * A simple abstraction for a parsed HTTP response returned by a hot function
     */
    public interface ParsedHttpResponse {
        /**
         * Return the body of the function result as a byte array
         *
         * @return the function response body
         */
        byte[] getBodyAsBytes();

        /**
         * return the body of the function response as a string
         *
         * @return a function response body
         */
        String getBodyAsString();

        /**
         * A map of he headers returned by the function
         * <p>
         * These are squashed so duplicated headers will be ignored (takes the first header)
         *
         * @return a map of headers
         */
        Map<String, String> getHeaders();

        /**
         * @return the HTTP status code returned by the function
         */
        int getStatus();
    }

    /**
     * Parses any pending HTTP responses on the functions stdout stream
     *
     * @return a list of Parsed HTTP responses from the function runtime output;
     */
    public List<ParsedHttpResponse> getParsedHttpResponses() {
        return getParsedHttpResponses(stdOut.toByteArray());
    }

    public static List<ParsedHttpResponse> getParsedHttpResponses(byte[] streamAsBytes) {

        SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        ByteArrayInputStream parseStream = new ByteArrayInputStream(streamAsBytes);
        sib.bind(parseStream);

        DefaultHttpResponseParser parser = new DefaultHttpResponseParser(sib);
        List<ParsedHttpResponse> responses = new ArrayList<>();

        while (true) {
            try {
                HttpResponse response = parser.parse();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ContentLengthInputStream cis = new ContentLengthInputStream(sib, Long.parseLong(response.getFirstHeader("Content-length").getValue()));

                IOUtils.copy(cis, bos);
                cis.close();
                byte[] body = bos.toByteArray();
                ParsedHttpResponse r = new ParsedHttpResponse() {
                    @Override
                    public byte[] getBodyAsBytes() {
                        return body;
                    }

                    @Override
                    public String getBodyAsString() {
                        return new String(body);
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        Arrays.stream(response.getAllHeaders()).forEach((h) -> {
                            headers.put(h.getName(), h.getValue());
                        });
                        return headers;
                    }

                    @Override
                    public int getStatus() {
                        return response.getStatusLine().getStatusCode();
                    }
                };
                responses.add(r);
            } catch (NoHttpResponseException e) {
                break;
            } catch (Exception e) {
                throw new RuntimeException("Invalid HTTP response", e);
            }
        }
        return responses;

    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    private final class DefaultEventBuilder extends EventBuilder {
        boolean sent = false;

        @Override
        public void enqueue() {
            if (sent) {
                throw new IllegalStateException("Cannot enqueue multiple default events ");
            }
            vars.putAll(commonEnv());
            vars.put("FN_REQUEST_URL", requestUrl);
            vars.put("FN_CALL_ID", "call-id");
            vars.put("FN_METHOD",method);
            headers.forEach((k, v) -> {
                vars.put("FN_HEADER_" + k.toUpperCase().replaceAll("-", "_"), v);
            });
            pendingInput = body;
            sent = true;


        }
    }

    /**
     * mock a default event (Input and stdOut encoded as stdin/stdout)
     *
     * @return a new event builder.
     */
    public EventBuilder givenDefaultEvent() {
        return new DefaultEventBuilder();
    }

    /**
     * mock an http event
     */
    public EventBuilder givenHttpEvent() {
        return new HttpEventBuilder();
    }

}
