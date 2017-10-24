package com.fnproject.fn.runtime;


import com.fnproject.fn.api.FunctionInvoker;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.flow.FlowContinuationInvoker;
import com.fnproject.fn.runtime.exception.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Main entry point
 */
public class EntryPoint {


    public static void main(String... args) throws Exception {
        PrintStream originalSystemOut = System.out;
        // Override stdout while the function is running, so that the function result can be serialized to stdout
        // without interference from the user printing stuff to what they believe is stdout.
        System.setOut(System.err);
        int exitCode = new EntryPoint().run(
                System.getenv(),
                System.in,
                originalSystemOut,
                System.err,
                args);
        System.setOut(originalSystemOut);
        System.exit(exitCode);
    }


    private List<FunctionInvoker> configuredInvokers = Arrays.asList(new FlowContinuationInvoker(), new MethodFunctionInvoker());


    /**
     * Entrypoint runner - this executes the whole lifecycle of the fn Java FDK runtime - including multiple invocations in the function for hot functions
     *
     * @return the desired process exit status
     */
    public int run(Map<String, String> env, InputStream functionInput, OutputStream functionOutput, PrintStream loggingOutput, String... args) {
        if (args.length != 1) {
            throw new InvalidEntryPointException("Expected one argument, of the form com.company.project.MyFunctionClass::myFunctionMethod");
        }
        // TODO better parsing of class stuff
        String[] classMethod = args[0].split("::");

        if (classMethod.length != 2) {
            throw new InvalidEntryPointException("Entry point is malformed expecting a string of the form com.company.project.MyFunctionClass::myFunctionMethod");
        }
        String cls = classMethod[0];
        String mth = classMethod[1];

        int lastStatus = 0;
        try {
            final Map<String, String> configFromEnvVars = Collections.unmodifiableMap(excludeInternalConfigAndHeaders(env));

            FunctionLoader functionLoader = new FunctionLoader();
            FunctionRuntimeContext runtimeContext = functionLoader.loadFunction(cls, mth, configFromEnvVars);

            String format = env.get(Codecs.FN_FORMAT);
            EventCodec codec;

            if (format != null && format.equalsIgnoreCase("http")) {
                codec = new HttpEventCodec(env,functionInput, functionOutput);
            } else if (format == null || format.equalsIgnoreCase("default")) {
                codec = new DefaultEventCodec(env, functionInput, functionOutput);
            } else {
                throw new FunctionInputHandlingException("Unsupported function format:" + format);
            }

            do {
                try {
                    Optional<InputEvent> evtOpt = codec.readEvent();
                    if (!evtOpt.isPresent()) {
                        break;
                    }
                    FunctionInvocationContext fic = new FunctionInvocationContext(runtimeContext);
                    try (InputEvent evt = evtOpt.get()) {
                        OutputEvent output = null;
                        for (FunctionInvoker invoker : configuredInvokers) {
                            Optional<OutputEvent> result = invoker.tryInvoke(fic, evt);
                            if (result.isPresent()) {
                                output = result.get();
                                break;
                            }
                        }
                        if (output == null) {
                            throw new FunctionInputHandlingException("No invoker found for input event");
                        }
                        codec.writeEvent(output);
                        if (output.isSuccess()) {
                            lastStatus = 0;
                            fic.fireOnSuccessfulInvocation();
                        } else {
                            lastStatus = 1;
                            fic.fireOnFailedInvocation();
                        }
                    } catch (IOException e) {
                        fic.fireOnFailedInvocation();
                        throw new FunctionInputHandlingException("Error closing function input", e);
                    } catch (Exception e) {
                        // Make sure we commit any pending Flows, then rethrow
                        fic.fireOnFailedInvocation();
                        throw e;
                    }

                } catch (InternalFunctionInvocationException fie) {
                    loggingOutput.println("An error occurred in function: " + filterStackTraceToOnlyIncludeUsersCode(fie));
                    codec.writeEvent(fie.toOutput());

                    // Here: completer-invoked continuations are *always* reported as successful to the Fn platform;
                    // the completer interprets the embedded HTTP-framed response.
                    lastStatus = fie.toOutput().isSuccess() ? 0 : 1;
                }
            } while (codec.shouldContinue());
        } catch (FunctionLoadException | FunctionInputHandlingException | FunctionOutputHandlingException e) {
            // catch all block;
            loggingOutput.println(filterStackTraceToOnlyIncludeUsersCode(e));
            return 2;
        }

        return lastStatus;
    }


    /**
     * Produces a string representation of the supplied Throwable.
     * <p>
     * Exception causes are walked until the end, each exception has its stack walked until either:
     * - the end, or
     * - a class in `com.fnproject.fn`
     * <p>
     * This means the user sees a full stack trace of their code, messages without stacktraces from
     * the layers of com.fnproject.fn which are passed through, with the root cause at the top of
     * the trace
     */
    private String filterStackTraceToOnlyIncludeUsersCode(Throwable t) {

        StringBuilder sb = new StringBuilder();
        Throwable current = t;

        while (current != null) {
            addExceptionToStringBuilder(sb, current);
            current = current.getCause();
        }

        return sb.toString();

    }

    private void addExceptionToStringBuilder(StringBuilder sb, Throwable t) {

        if (t.toString().startsWith("com.fnproject.fn")) {
            // This elides the FQCN of the exception class if it's from our runtime.
            sb.append(t.getMessage());
        } else {
            sb.append("Caused by: " + t.toString());
        }

        for (StackTraceElement elem : t.getStackTrace()) {
            if (elem.getClassName().startsWith("com.fnproject.fn")) {
                break;
            }
            sb.append("\n    at " + elem.toString());
        }

        sb.append("\n");
    }


    /**
     * @return a map of all the values in env having excluded any internal config variables that the platform uses and
     * any headers that were added to env. Headers are identified as being variables prepended with 'HEADER_'.
     */
    private Map<String, String> excludeInternalConfigAndHeaders(Map<String, String> env) {
        Set<String> nonConfigEnvKeys = new HashSet<>(Arrays.asList("fn_app_name", "fn_path", "fn_method", "fn_request_url",
                "fn_format", "content-length", "fn_call_id"));
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String lowerCaseKey = entry.getKey().toLowerCase();
            if (!lowerCaseKey.startsWith("header_") && !nonConfigEnvKeys.contains(lowerCaseKey)) {
                config.put(entry.getKey(), entry.getValue());
            }
        }
        return config;
    }
}

