package com.fnproject.fn.runtime;


import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionLoadException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import com.fnproject.fn.runtime.exception.InvalidEntryPointException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main entry point
 */
public class EntryPoint {


    public static void main(String... args) throws Exception {
        PrintStream originalSystemOut = System.out;
        // Override stdout while the function is running, so that the function result can be serialized to stdout
        // without interference from the user printing stuff to what they believe is stdout.
        System.setOut(System.err);


        String format = System.getenv("FN_FORMAT");
        EventCodec codec;
        if (format != null && format.equalsIgnoreCase("http")) {
            codec = new HttpEventCodec(System.getenv(), System.in, originalSystemOut);
        } else if (format == null || format.equalsIgnoreCase("default")) {
            codec = new DefaultEventCodec(System.getenv(), System.in, originalSystemOut);
        } else if (format.equals("httpstream")) {
            codec = new HTTPStreamCodec(System.getenv());
        } else {
            throw new FunctionInputHandlingException("Unsupported function format:" + format);
        }


        int exitCode = new EntryPoint().run(
          System.getenv(),
          codec,
          System.err,
          args);
        System.setOut(originalSystemOut);
        System.exit(exitCode);
    }

    /**
     * Entrypoint runner - this executes the whole lifecycle of the fn Java FDK runtime - including multiple invocations in the function for hot functions
     *
     * @return the desired process exit status
     */
    public int run(Map<String, String> env, EventCodec codec, PrintStream loggingOutput, String... args) {
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

        // TODO deprecate with default contract
        final AtomicInteger lastStatus = new AtomicInteger();
        try {
            final Map<String, String> configFromEnvVars = Collections.unmodifiableMap(excludeInternalConfigAndHeaders(env));

            FunctionLoader functionLoader = new FunctionLoader();
            FunctionRuntimeContext runtimeContext = new FunctionRuntimeContext(functionLoader.loadClass(cls, mth), configFromEnvVars);

            FunctionConfigurer functionConfigurer = new FunctionConfigurer();
            functionConfigurer.configure(runtimeContext);


            codec.runCodec((evt) -> {
                try {
                    FunctionInvocationContext fic = runtimeContext.newInvocationContext();
                    try (InputEvent myEvt = evt) {
                        OutputEvent output = runtimeContext.tryInvoke(evt, fic);
                        if (output == null) {
                            throw new FunctionInputHandlingException("No invoker found for input event");
                        }
                        if (output.isSuccess()) {
                            lastStatus.set(0);
                            fic.fireOnSuccessfulInvocation();
                        } else {
                            lastStatus.set(1);
                            fic.fireOnFailedInvocation();
                        }
                        return output;

                    } catch (IOException err) {
                        fic.fireOnFailedInvocation();
                        throw new FunctionInputHandlingException("Error closing function input", err);
                    } catch (Exception e) {
                        // Make sure we commit any pending Flows, then rethrow
                        fic.fireOnFailedInvocation();
                        throw e;
                    }
                } catch (InternalFunctionInvocationException fie) {
                    loggingOutput.println("An error occurred in function: " + filterStackTraceToOnlyIncludeUsersCode(fie));
                    // Here: completer-invoked continuations are *always* reported as successful to the Fn platform;
                    // the completer interprets the embedded HTTP-framed response.
                    lastStatus.set(fie.toOutput().isSuccess() ? 0 : 1);
                    return fie.toOutput();
                }

            });
        } catch (FunctionLoadException | FunctionInputHandlingException | FunctionOutputHandlingException e) {
            // catch all block;
            loggingOutput.println(filterStackTraceToOnlyIncludeUsersCode(e));
            return 2;
        } catch (Exception ee) {
            loggingOutput.println("An unexpected error occurred:");
            ee.printStackTrace(loggingOutput);
            return 1;
        }

        return lastStatus.get();
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

