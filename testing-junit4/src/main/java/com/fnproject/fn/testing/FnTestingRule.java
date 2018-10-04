package com.fnproject.fn.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.EventCodec;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;
import java.util.*;


/**
 * Testing {@link Rule} for fn Java FDK functions.
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
 *            .addHeader("FOO", "BAR")
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
    private List<InputEvent> pendingInput = Collections.synchronizedList(new ArrayList<>());
    private List<FnResult> output = Collections.synchronizedList(new ArrayList<>());
    private ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> sharedPrefixes = new ArrayList<>();
    private int lastExitCode;
    private final List<FnTestingRuleFeature> features = new ArrayList<>();

    {
        // Internal shared classes required to bridge completer into tests
        addSharedClassPrefix("java.");
        addSharedClassPrefix("javax.");
        addSharedClassPrefix("sun.");
        addSharedClassPrefix("jdk.");


        addSharedClass(Headers.class);
        addSharedClass(InputEvent.class);
        addSharedClass(OutputEvent.class);
        addSharedClass(OutputEvent.Status.class);
        addSharedClass(TestOutput.class);
        addSharedClass(TestCodec.class);
        addSharedClass(EventCodec.class);
        addSharedClass(EventCodec.Handler.class);



    }

    /**
     * TestOutput represents an output of a function it wraps OutputEvent and provides buffered access to the function output
     */
    public static final class TestOutput implements FnResult {
        private final OutputEvent from;
        byte[] body;

        private TestOutput(OutputEvent from) throws IOException {
            this.from = Objects.requireNonNull(from, "from");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            from.writeToOutput(bos);
            body = bos.toByteArray();
        }

        /**
         * construct a test output from an output event - this consums the body of the output event
         *
         * @param from an output event to consume
         * @return a new TestEvent that wraps the passed even t
         * @throws IOException
         */
        public static TestOutput fromOutputEvent(OutputEvent from) throws IOException {
            return new TestOutput(from);
        }

        @Override
        public Status getStatus() {
            return from.getStatus();
        }

        @Override
        public Optional<String> getContentType() {
            return from.getContentType();
        }

        @Override
        public Headers getHeaders() {
            return from.getHeaders();
        }

        @Override
        public void writeToOutput(OutputStream out) throws IOException {
            out.write(body);
        }


        @Override
        public byte[] getBodyAsBytes() {
            return body;
        }

        /**
         * Returns the buffered body of the event as a string
         *
         * @return the body of the event as a string
         */
        @Override
        public String getBodyAsString() {
            return new String(body);
        }

    }

    private FnTestingRule() {
    }


    public void addFeature(FnTestingRuleFeature f) {
        this.features.add(f);
    }

    /**
     * Create an instance of the testing {@link Rule}, with Flows support
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
    public FnEventBuilderJUnit4 givenEvent() {
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


    public static class TestCodec implements EventCodec {
        private final List<InputEvent> input;
        private final List<FnResult> output;

        public TestCodec(List<InputEvent> input, List<FnResult> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void runCodec(Handler h) {
            for (InputEvent in : input) {
                try {
                    output.add(new TestOutput(h.handle(in)));
                } catch (IOException e) {
                    throw new RuntimeException("Unexpected exception in test", e);
                }
            }
        }
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

        for (FnTestingRuleFeature f : features) {
            f.prepareTest(functionClassLoader, oldSystemErr, cls, method);
        }

        Map<String, String> mutableEnv = new HashMap<>();

        try {
            PrintStream functionErr = new PrintStream(new TeeOutputStream(stdErr, oldSystemErr));
            System.setOut(functionErr);
            System.setErr(functionErr);

            mutableEnv.putAll(config);
            mutableEnv.putAll(eventEnv);
            mutableEnv.put("FN_FORMAT", "http-stream");
            mutableEnv.put("FN_FN_ID","myFnID");
            mutableEnv.put("FN_APP_ID","myAppID");

            FnTestingClassLoader forked = new FnTestingClassLoader(functionClassLoader, sharedPrefixes);
            if (forked.isShared(cls)) {
                oldSystemErr.println("WARNING: The function class under test is shared with the test ClassLoader.");
                oldSystemErr.println("         This may result in unexpected behaviour around function initialization and configuration.");
            }
            for (FnTestingRuleFeature f : features) {
                f.prepareFunctionClassLoader(forked);
            }

            TestCodec codec = new TestCodec(pendingInput, output);

            lastExitCode = forked.run(
              mutableEnv,
              codec,
              functionErr,
              cls + "::" + method);

            stdErr.flush();

            for (FnTestingRuleFeature f : features) {
                f.afterTestComplete();
            }
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
     * Get the exit code from the most recent invocation
     * 0 = success
     * 1 = failed
     * 2 = not run due to initialization error
     */
    public int getLastExitCode() {
        return lastExitCode;
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
        return output;
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


    public List<String> getSharedPrefixes() {
        return Collections.unmodifiableList(sharedPrefixes);
    }

    public Map<String, String> getConfig() {
        return Collections.unmodifiableMap(config);
    }

    public Map<String, String> getEventEnv() {
        return Collections.unmodifiableMap(eventEnv);
    }


    /**
     * Builds a mocked input event into the function runtime
     */
    private class DefaultFnEventBuilder implements FnEventBuilderJUnit4 {

        FnHttpEventBuilder builder = new FnHttpEventBuilder();


        @Override
        public FnEventBuilder withHeader(String key, String value) {
            builder.withHeader(key, value);
            return this;
        }

        @Override
        public FnEventBuilder withBody(InputStream body) throws IOException {
            builder.withBody(body);
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
        public FnTestingRule enqueue() {

            pendingInput.add(builder.buildEvent());
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
