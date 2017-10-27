package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.io.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    private Map<String, FnFunctionStub> functionStubs = new HashMap<>();
    public static InMemCompleter completer = null;

    private final List<String> sharedPrefixes = new ArrayList<>();

    {
        // Internal shared classes required to bridge completer into tests
        addSharedClassPrefix("java.");
        addSharedClassPrefix("javax.");
        addSharedClassPrefix("sun.");
        addSharedClassPrefix("jdk.");

        addSharedClass(CompleterClient.class);
        addSharedClass(CompleterClientFactory.class);
        addSharedClass(CompletionId.class);
        addSharedClass(FlowId.class);
        addSharedClass(CompleterClient.ExternalCompletion.class);
        addSharedClass(CodeLocation.class);
        addSharedClass(Headers.class);
        addSharedClass(HttpMethod.class);
        addSharedClass(com.fnproject.fn.api.flow.HttpRequest.class);
        addSharedClass(com.fnproject.fn.api.flow.HttpResponse.class);
        addSharedClass(QueryParameters.class);
        addSharedClass(InputEvent.class);
        addSharedClass(OutputEvent.class);
        addSharedClass(FlowCompletionException.class);
        addSharedClass(FunctionInvocationException.class);
        addSharedClass(ExternalCompletionException.class);
        addSharedClass(PlatformException.class);

    }

    private FnTestingRule() {
    }

    /**
     * Create an instance of the testing {@link org.junit.Rule}, with Flows support
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

    /**
     * Add a class or package name to be forked during the test.
     * The test will be run under the aegis of a classloader that duplicates the class hierarchy named.
     *
     * @param prefix A class name or package prefix, such as "com.example.foo."
     */
    public FnTestingRule addSharedClassPrefix(String prefix) {
        sharedPrefixes.add(prefix);
        return this;
    }

    /**
     * Add a class to be forked during the test.
     * The test will be run under the aegis of a classloader that duplicates the class hierarchy named.
     *
     * @param cls A class
     */
    public FnTestingRule addSharedClass(Class<?> cls) {
        sharedPrefixes.add("=" + cls.getName());
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
     * Runs the function runtime with the specified class and method (and waits for Flow stages to finish
     * if the test spawns any flows)
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(Class<?> cls, String method) {
        thenRun(cls.getName(), method);
    }


    /**
     * Runs the function runtime with the specified class and method (and waits for Flow stages to finish
     * if the test spawns any Flow)
     *
     * @param cls    class to thenRun
     * @param method the method name
     */
    public void thenRun(String cls, String method) {
        final ClassLoader functionClassLoader;
        Class c = null;
        try {
            // Trick to work around Maven class loader separation
            // if passed class is a valid class then set the classloader to the same as the class's loader
            c = Class.forName(cls);
        } catch (Exception ignored) {
            // TODO don't fall through here
        }
        if (c != null) {
            functionClassLoader = c.getClassLoader();
        } else {
            functionClassLoader = getClass().getClassLoader();
        }

        PrintStream oldSystemOut = System.out;
        PrintStream oldSystemErr = System.err;

        InMemCompleter.CompleterInvokeClient client = new TestRuleCompleterInvokeClient(functionClassLoader, oldSystemErr, cls, method);

        InMemCompleter.FnInvokeClient fnInvokeClient = new TestRuleFnInvokeClient();

        // FlowContinuationInvoker.setTestingMode(true);
        // The following must be a static: otherwise the factory (the lambda) will not be serializable.
        completer = new InMemCompleter(client, fnInvokeClient);

        //TestSupport.installCompleterClientFactory(completer, oldSystemErr);


        Map<String, String> mutableEnv = new HashMap<>();

        try {
            PrintStream functionOut = new PrintStream(stdOut);
            PrintStream functionErr = new PrintStream(new TeeOutputStream(stdErr, oldSystemErr));
            System.setOut(functionErr);
            System.setErr(functionErr);

            mutableEnv.putAll(config);
            mutableEnv.putAll(eventEnv);
            mutableEnv.put("FN_FORMAT", "http");

            FnTestingClassLoader forked = new FnTestingClassLoader(functionClassLoader, sharedPrefixes);
            if (forked.isShared(cls)) {
                oldSystemErr.println("WARNING: The function class under test is shared with the test ClassLoader.");
                oldSystemErr.println("         This may result in unexpected behaviour around function initialization and configuration.");
            }
            forked.setCompleterClient(completer);
            forked.run(
               mutableEnv,
               pendingInput,
               functionOut,
               functionErr,
               cls + "::" + method);

            stdOut.flush();
            stdErr.flush();

            completer.awaitTermination();
        } catch (Exception e) {
            throw new RuntimeException("internal error raised by entry point or flushing the test streams", e);
        } finally {
            System.out.flush();
            System.err.flush();
            System.setOut(oldSystemOut);
            System.setErr(oldSystemErr);

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
        return parseHttpStreamForResults(stdOut.toByteArray());
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


    private List<FnResult> parseHttpStreamForResults(byte[] httpStream) {
        SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
        ByteArrayInputStream parseStream = new ByteArrayInputStream(httpStream);
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


    public FnFunctionStubBuilder givenFn(String id) {
        return new FnFunctionStubBuilder() {
            @Override
            public FnTestingRule withResult(byte[] result) {
                return withAction((body) -> result);
            }

            @Override
            public FnTestingRule withFunctionError() {
                return withAction((body) -> {
                    throw new FunctionError("simulated by testing platform");
                });
            }

            @Override
            public FnTestingRule withPlatformError() {
                return withAction((body) -> {
                    throw new PlatformError("simulated by testing platform");
                });
            }

            @Override
            public FnTestingRule withAction(ExternalFunctionAction f) {
                functionStubs.put(id, (HttpMethod method, Headers headers, byte[] body) -> {
                    try {
                        return Result.success(new Datum.HttpRespDatum(200, Headers.emptyHeaders(), f.apply(body)));
                    } catch (FunctionError functionError) {
                        throw new ResultException(new Datum.HttpRespDatum(500, Headers.emptyHeaders(), functionError.getMessage().getBytes()));
                    } catch (PlatformError platformError) {
                        throw new ResultException(new Datum.ErrorDatum(Datum.ErrorType.function_invoke_failed, platformError.getMessage()));
                    }
                });
                return FnTestingRule.this;
            }
        };
    }

    private interface FnFunctionStub {
        Result stubFunction(HttpMethod method, Headers headers, byte[] body);
    }

    /**
     * Builds a mocked input event into the function runtime
     */
    private class DefaultFnEventBuilder implements FnEventBuilder {

        FnHttpEventBuilder builder = new FnHttpEventBuilder()
           .withMethod("GET")
           .withCallId("call-id")
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

    private class TestRuleCompleterInvokeClient implements InMemCompleter.CompleterInvokeClient {
        private final ClassLoader functionClassLoader;
        private final PrintStream oldSystemErr;
        private final String cls;
        private final String method;
        private final Set<FnTestingClassLoader> pool = new HashSet<>();


        public TestRuleCompleterInvokeClient(ClassLoader functionClassLoader, PrintStream oldSystemErr, String cls, String method) {
            this.functionClassLoader = functionClassLoader;
            this.oldSystemErr = oldSystemErr;
            this.cls = cls;
            this.method = method;
        }


        @Override
        public Result invokeStage(String fnId, FlowId flowId, CompletionId stageId, Datum.Blob closure, List<Result> input) {
            // Construct a new ClassLoader hierarchy with a copy of the statics embedded in the runtime.
            // Initialise it appropriately.
            FnTestingClassLoader fcl = new FnTestingClassLoader(functionClassLoader, sharedPrefixes);
            fcl.setCompleterClient(completer);


            // TODO avoid repeating MIME stuff (library-ise with CT/runtime? )
            // TODO de-dupe shared env setup
            // TODO Making up event details (app path?)

            byte[] inputBody;
            String boundary = UUID.randomUUID().toString().replaceAll("-", "");

            try {
                ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
                bodyBytes.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                new Datum.BlobDatum(closure).writePart(bodyBytes);

                for (Result r : input) {
                    bodyBytes.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    r.writePart(bodyBytes);
                }
                bodyBytes.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                inputBody = bodyBytes.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write inputBody", e);
            }

            // oldSystemErr.println("Body\n" + new String(inputBody));

            InputStream is = new FnHttpEventBuilder()
               .withBody(inputBody)
               .withAppName("appName")
               .withRoute("/route").withRequestUrl("http://some/url")
               .withMethod("POST")
               .withHeader(RemoteCompleterApiClient.CONTENT_TYPE_HEADER, String.format("multipart/mixed; boundary=\"%s\"", boundary))
               .withHeader(RemoteCompleterApiClient.FLOW_ID_HEADER, flowId.getId()).withHeader(RemoteCompleterApiClient.STAGE_ID_HEADER, stageId.getId()).currentEventInputStream();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Map<String, String> mutableEnv = new HashMap<>();
            PrintStream functionOut = new PrintStream(output);
            PrintStream functionErr = new PrintStream(oldSystemErr);

            // Do we want to capture IO from continuations on the main log stream?
            // System.setOut(functionErr);
            // System.setErr(functionErr);

            mutableEnv.putAll(config);
            mutableEnv.putAll(eventEnv);
            mutableEnv.put("FN_FORMAT", "http");


            fcl.run(
               mutableEnv,
               is,
               functionOut,
               functionErr,
               cls + "::" + method);


            SessionInputBufferImpl sib = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
            ByteArrayInputStream parseStream = new ByteArrayInputStream(output.toByteArray());
            sib.bind(parseStream);
            DefaultHttpResponseParser parser = new DefaultHttpResponseParser(sib);
            Result r;
            try {
                // Read wrapping result, and throw it away
                parser.parse();
                IdentityInputStream iis = new IdentityInputStream(sib);
                byte[] responseBody = IOUtils.toByteArray(iis);

                // System.err.println("Got Fn response body: \n" + new String(responseBody));
                // HTTP in HTTP :(
                SessionInputBufferImpl sib2 = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 65535);
                InputStream frameStream = new ByteArrayInputStream(responseBody);
                sib2.bind(frameStream);

                DefaultHttpResponseParser parser2 = new DefaultHttpResponseParser(sib2);
                HttpResponse wrappedResponse = parser2.parse();

                IdentityInputStream frameBodyStream = new IdentityInputStream(sib2);

                wrappedResponse.setEntity(new InputStreamEntity(frameBodyStream));

                r = Result.readResult(wrappedResponse);


            } catch (Exception e) {
                oldSystemErr.println("Err\n" + e);
                e.printStackTrace(oldSystemErr);
                r = Result.failure(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Error reading fn Response:" + e.getMessage()));
            }

            if (!r.isSuccess()) {
                throw new ResultException(r.getDatum());
            }
            return r;

        }
    }

    private class TestRuleFnInvokeClient implements InMemCompleter.FnInvokeClient {
        @Override
        public CompletableFuture<Result> invokeFunction(String fnId, HttpMethod method, Headers headers, byte[] data) {
            return CompletableFuture.completedFuture(functionStubs
               .computeIfAbsent(fnId, (k) -> {
                   throw new IllegalStateException("Function was invoked that had no definition: " + k);
               })
               .stubFunction(method, headers, data));
        }
    }
}
