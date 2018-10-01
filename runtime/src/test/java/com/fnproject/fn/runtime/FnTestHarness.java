package com.fnproject.fn.runtime;


import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Function internal testing harness - this provides access the call-side of the functions contract excluding the codec which is mocked
 */
public class FnTestHarness implements TestRule {
    private final List<InputEvent> pendingInput = Collections.synchronizedList(new ArrayList<>());
    private final List<TestOutput> output = Collections.synchronizedList(new ArrayList<>());
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
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
     * @param key the configuration key
     */
    public String getConfig(String key) {
        return config.get(key.toUpperCase().replaceAll("[- ]", "_"));
    }

    public String getOnlyOutputAsString() {
        if (output.size() != 1) {
            throw new IllegalStateException("expecting exactly one result, got " + output.size());
        }
        return new String(output.get(0).getBody());
    }

    /**
     * Builds a mocked input event into the function runtime
     */
    public final class EventBuilder {
        InputStream body = new ByteArrayInputStream(new byte[0]);
        String contentType = null;
        String callID = "callID";
        Instant deadline = Instant.now().plus(1, ChronoUnit.HOURS);
        Headers headers = Headers.emptyHeaders();

        /**
         * Add a header to the input
         * Duplicate headers will be overwritten
         *
         * @param key   header key
         * @param v1 header value
         * @param vs other header values
         */
        public EventBuilder withHeader(String key, String v1, String... vs) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(v1, "value");
            Objects.requireNonNull(vs, "vs");
            Arrays.stream(vs).forEach(v -> Objects.requireNonNull(v, "null value in varags list "));
            headers = headers.addHeader(key, v1);
            for (String v : vs) {
                headers = headers.addHeader(key, v);
            }

            return this;
        }

        /**
         * Add a series of headers to the input
         * This may override duplicate headers
         *
         * @param headers Map of headers to add
         */
        public EventBuilder withHeaders(Map<String, String> headers) {
            headers.forEach(this::withHeader);
            return this;
        }

        /**
         * Set the body of the request by providing an InputStream
         *
         * @param body the bytes of the body
         */
        public EventBuilder withBody(InputStream body) {
            Objects.requireNonNull(body, "body");

            this.body = body;
            return this;
        }

        /**
         * Set the body of the request as a byte array
         *
         * @param body the bytes of the body
         */
        public EventBuilder withBody(byte[] body) {
            Objects.requireNonNull(body, "body");
            return withBody(new ByteArrayInputStream(body));
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
         * Prepare an event for the configured codec - this sets appropriate environment variable in the Env mock and StdIn mocks.
         * <p>
         *
         * @throws IllegalStateException If the the codec only supports one event and an event has already been enqueued.
         */
        public void enqueue() {
            InputEvent event = new ReadOnceInputEvent(body, headers, callID, deadline);
            pendingInput.add(event);

        }

        Map<String, String> commonEnv() {
            Map<String, String> env = new HashMap<>(config);
            env.put("FN_APP_ID", "appID");
            env.put("FN_FN_ID", "fnID");

            return env;
        }
    }

    static class TestOutput implements OutputEvent {
        private final OutputEvent from;
        byte[] body;

        TestOutput(OutputEvent from) throws IOException {
            this.from = from;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            from.writeToOutput(bos);
            body = bos.toByteArray();
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

        public byte[] getBody() {
            return body;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TestOutput{");
            sb.append("body=");
            if (body == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < body.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(body[i]);
                sb.append(']');
            }
            sb.append(", status=").append(getStatus());
            sb.append(", contentType=").append(getContentType());
            sb.append(", headers=").append(getHeaders());
            sb.append('}');
            return sb.toString();
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

    static class TestCodec implements EventCodec {
        private final List<InputEvent> input;
        private final List<TestOutput> output;

        TestCodec(List<InputEvent> input, List<TestOutput> output) {
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
            PrintStream functionErr = new PrintStream(new TeeOutputStream(stdErr, oldSystemErr));
            System.setOut(functionErr);
            System.setErr(functionErr);

            Map<String, String> fnConfig = new HashMap<>(config);
            fnConfig.put("FN_APP_ID", "appID");
            fnConfig.put("FN_FORMAT", "http-stream");
            fnConfig.put("FN_FN_ID", "fnID");


            exitStatus = new EntryPoint().run(
              fnConfig,
              new TestCodec(pendingInput, output),
              functionErr,
              cls + "::" + method);
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
        return new EventBuilder();
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
    public List<TestOutput> getOutputs() {
        return output;
    }


    @Override
    public Statement apply(Statement base, Description description) {
        return base;

    }


}
