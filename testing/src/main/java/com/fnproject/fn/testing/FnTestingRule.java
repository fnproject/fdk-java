package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.FunctionConfigurer;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.runtime.cloudthreads.CloudThreadsContinuationInvoker;
import com.fnproject.fn.runtime.cloudthreads.CompleterClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;
import java.util.*;

/**
 * Testing {@link org.junit.Rule} for fn Java FDK functions.
 * <p>
 * This interface facilitates:
 * <ul>
 * <li>The creation of an in-memory environment replicating the functionality of the {@code fn} service</li>
 * <li>The creation of input events passed to a user function using {@link #givenEvent()}</li>
 * <li>The verification of function behaviour by accessing output represented by {@link FnResult} instances.</li>
 * </ul>
 * <h1>Example Usage:</h1>
 * <pre>{@code
 * public class MyFunctionTest {
 *     {@literal @}Rule
 *     public final FnTestingRule testing = FnTestingRule.createDefault();
 *
 *     {@literal @}Test
 *     public void myTest() {
 *         // Create an event to invoke MyFunction and put it into the event queue
 *         fn.givenEvent()
 *            .withAppName("alpha")
 *            .withRoute("/bravo")
 *            .withRequestUrl("http://charlie/alpha/bravo")
 *            .withMethod("POST")
 *            .withHeader("FOO", "BAR")
 *            .withBody("Body")
 *            .enqueue();
 *
 *         // Run MyFunction#handleRequest using the built event queue from above
 *         fn.thenRun(MyFunction.class, "handleRequest");
 *
 *         // Get the function result and check it for correctness
 *         FnResult result = fn.getOnlyResult();
 *         assertThat(result.getStatus()).isEqualTo(200);
 *         assertThat(result.getBodyAsString()).isEqualTo("expected return value of my function");
 *     }
 * }}</pre>
 */
public final class FnTestingRule implements TestRule {
    private final Map<String, String> config = new HashMap<>();
    private Map<String, String> eventEnv = new HashMap<>();
    private boolean hasEvents = false;
    private InputStream pendingInput = new ByteArrayInputStream(new byte[0]);
    private ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private String entryPoint = null;

    private FnTestingRule() { }

    /**
     * Create an instance of the testing {@link org.junit.Rule}, without Cloud Threads support
     *
     * @return a new test rule
     */
    public static FnTestingRule createDefault() {
        return new FnTestingRule();
    }

    /**
     * Add a config variable to the function for the test
     * <p>
     * Config names will be translated to upper case with hyphens and spaces translated to _. Clashing config keys will
     * be overwritten.
     *
     * @param key   the configuration key
     * @param value the configuration value
     * @return the current test rule
     */
    public FnTestingRule setConfig(String key, String value) {
        config.put(key.toUpperCase().replaceAll("[- ]", "_"), value);
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return base;
    }

    /**
     * Create an HTTP event builder for the function
     *
     * @return a new event builder
     */
    public FnEventBuilder givenEvent() {
        return new DefaultFnEventBuilder();
    }

    /**
     * Runs the function runtime with the specified class and method (and waits for Cloud Threads completions to finish
     * if the test spawns any Cloud Thread)
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(Class<?> cls, String method) {
        thenRun(cls.getName(), method);
    }

    /**
     * Runs the function runtime with the specified class and method (and waits for Cloud Threads completions to finish
     * if the test spawns any Cloud Thread)
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(String cls, String method) {
        try {
            // Trick to work around Maven class loader separation
            // if passed class is a valid class then set the classloader to the same as the class's loader
            Class c = Class.forName(cls);
            FunctionConfigurer.setContextClassLoader(c.getClassLoader());
        } catch (Exception ignored) {
        }

        Map<String, String> mutableEnv = new HashMap<>();
        PrintStream oldSystemOut = System.out;
        PrintStream oldSystemErr = System.err;
        try {
            this.entryPoint = cls + "::" + method;
            PrintStream functionOut = new PrintStream(stdOut);
            PrintStream functionErr = new PrintStream(new TeeOutputStream(stdErr, oldSystemErr));
            System.setOut(functionErr);
            System.setErr(functionErr);

            mutableEnv.putAll(config);
            mutableEnv.putAll(eventEnv);
            mutableEnv.put("FN_FORMAT", "http");

            CloudThreadsContinuationInvoker.setCompleterClientFactory((CompleterClientFactory) () -> {
                throw new IllegalStateException("Cannot test a function using Cloud Threads without completion support: use FnTestingRule.createWithCompletions()?");
            });

            new EntryPoint().run(
                    mutableEnv,
                    pendingInput,
                    functionOut,
                    functionErr,
                    cls + "::" + method);

            stdOut.flush();
            stdErr.flush();
        } catch (Exception e) {
            throw new RuntimeException("internal error raised by entry point or flushing the test streams", e);
        } finally {
            System.out.flush();
            System.err.flush();
            System.setOut(oldSystemOut);
            System.setErr(oldSystemErr);
            CloudThreadsContinuationInvoker.resetCompleterClientFactory();
            this.entryPoint = null;
        }
    }

    /**
     * Get the StdErr stream returned by the function as a byte array
     *
     * @return the StdErr stream as bytes from the runtime
     */
    public byte[] getStdErr() {
        return stdErr.toByteArray();
    }

    /**
     * Gets the StdErr stream returned by the function as a String
     *
     * @return the StdErr stream as a string from the function
     */
    public String getStdErrAsString() {
        return new String(stdErr.toByteArray());
    }

    /**
     * Parses any pending HTTP responses on the functions output stream and returns the corresponding FnResult instances
     *
     * @return a list of Parsed HTTP responses (as {@link FnResult}s) from the function runtime output
     */
    public List<FnResult> getResults() {
        return parseHttpStreamForResults(stdOut);
    }

    /**
     * Convenience method to get the one and only parsed http response expected on the output of the function
     *
     * @return a single parsed HTTP response from the function runtime output
     * @throws IllegalStateException if zero or more than one responses were produced
     */
    public FnResult getOnlyResult() {
        List<FnResult> results = getResults();
        if (results.size() == 1) {
            return results.get(0);
        }
        throw new IllegalStateException("One and only one response expected, but " + results.size() + " responses were generated.");
    }

    private List<FnResult> parseHttpStreamForResults(ByteArrayOutputStream httpStream) {
        SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        ByteArrayInputStream parseStream = new ByteArrayInputStream(httpStream.toByteArray());
        sib.bind(parseStream);

        DefaultHttpResponseParser parser = new DefaultHttpResponseParser(sib);
        List<FnResult> responses = new ArrayList<>();

        while (true) {
            try {
                HttpResponse response = parser.parse();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ContentLengthInputStream cis = new ContentLengthInputStream(sib, Long.parseLong(response.getFirstHeader("Content-length").getValue()));

                IOUtils.copy(cis, bos);
                cis.close();
                byte[] body = bos.toByteArray();
                FnResult r = new FnResult() {
                    @Override
                    public byte[] getBodyAsBytes() {
                        return body;
                    }

                    @Override
                    public String getBodyAsString() {
                        return new String(body);
                    }

                    @Override
                    public Headers getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        Arrays.stream(response.getAllHeaders()).forEach((h) ->
                                headers.put(h.getName(), h.getValue()));
                        return Headers.fromMap(headers);
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

    private FnResult parseHttpStreamForResult(ByteArrayOutputStream httpStream) {
        List<FnResult> continuationResults = parseHttpStreamForResults(httpStream);
        if (continuationResults.size() != 1) {
            throw new RuntimeException("Expected single HTTP response from the continuation invocation, received " + continuationResults.size());
        }
        return continuationResults.get(0);
    }

    /**
     * Builds a mocked input event into the function runtime
     */
    private class DefaultFnEventBuilder implements FnEventBuilder {

        FnHttpEventBuilder builder = new FnHttpEventBuilder().withMethod("GET")
                .withAppName("appName")
                .withRoute("/route")
                .withRequestUrl("http://example.com/r/appName/route");


        @Override
        public FnEventBuilder withHeader(String key, String value) {
            builder.withHeader(key, value);
            return this;
        }

        @Override
        public FnEventBuilder withBody(InputStream body, int contentLength) {
            builder.withBody(body, contentLength);
            return this;
        }

        @Override
        public FnEventBuilder withBody(byte[] body) {
            builder.withBody(body);
            return this;
        }

        @Override
        public FnEventBuilder withBody(String body) {
            builder.withBody(body);
            return this;
        }

        @Override
        public FnEventBuilder withAppName(String appName) {
            builder.withAppName(appName);
            return this;
        }

        @Override
        public FnEventBuilder withRoute(String route) {
            builder.withRoute(route);
            return this;
        }

        @Override
        public FnEventBuilder withMethod(String method) {
            builder.withMethod(method);
            return this;
        }

        @Override
        public FnEventBuilder withRequestUrl(String requestUrl) {
            builder.withRequestUrl(requestUrl);
            return this;

        }

        @Override
        public FnEventBuilder withQueryParameter(String key, String value) {
            builder.withQueryParameter(key, value);
            return this;
        }

        @Override
        public FnTestingRule enqueue() {

            // Only set env for first event.
            if (!hasEvents) {
                eventEnv.putAll(builder.currentEventEnv());
            }
            hasEvents = true;

            pendingInput = new SequenceInputStream(pendingInput, builder.currentEventInputStream());
            return FnTestingRule.this;
        }


        @Override
        public FnTestingRule enqueue(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("Invalid count");
            }
            for (int i = 0; i < n; i++) {
                enqueue();
            }
            return FnTestingRule.this;
        }


    }
}
