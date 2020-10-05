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


import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.FnFeatures;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeFeature;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionLoadException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;
import com.fnproject.fn.runtime.exception.FunctionInitializationException;
import com.fnproject.fn.runtime.exception.InternalFunctionInvocationException;
import com.fnproject.fn.runtime.exception.InvalidEntryPointException;

/**
 * Main entry point
 */
public class EntryPoint {


    // regex to sanitize version properties on the off chance they fall outside of acceptable header values
    private static final Pattern safeVersion = Pattern.compile("[^\\w ()._-]+");

    public static void main(String... args) {
        PrintStream originalSystemOut = System.out;
        System.setOut(System.err);

        String format = System.getenv("FN_FORMAT");
        EventCodec codec;
        if (format.equals(HTTPStreamCodec.HTTP_STREAM_FORMAT)) {
            // reduce risk of confusion due to non-header like system properties
            String jvmName = safeVersion.matcher(System.getProperty("java.vm.name")).replaceAll("");
            String jvmVersion = safeVersion.matcher(System.getProperty("java.version")).replaceAll("");
            String fdkVersion = "fdk-java/" + Version.FDK_VERSION +
                " (jvm=" + (jvmName + ", jvmv=" +
                jvmVersion + ")");

            codec = new HTTPStreamCodec(System.getenv(), fdkVersion);
        } else {
            throw new FunctionInputHandlingException("Unsupported function format:" + format);
        }

        int exitCode = new EntryPoint().run(System.getenv(), codec, System.err, args);
        System.setOut(originalSystemOut);
        System.exit(exitCode);
    }

    /**
     * Entry point runner - this executes the whole lifecycle of the fn Java FDK runtime - including multiple invocations in the function for hot functions
     *
     * @param env           the map of environment variables to run the function with (typically System.getenv but may be customised for testing)
     * @param codec         the codec to run the function with
     * @param loggingOutput the stream to send function error/logging to - this will be wrapped into System.err within the funciton
     * @param args          any further args passed to the entry point - specifically the class/method name
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

            MethodWrapper method = functionLoader.loadClass(cls, mth);
            FunctionRuntimeContext runtimeContext = new FunctionRuntimeContext(method, configFromEnvVars);
            FnFeature f = method.getTargetClass().getAnnotation(FnFeature.class);
            if (f != null) {
                enableFeature(runtimeContext, f);
            }
            FnFeatures fs = method.getTargetClass().getAnnotation(FnFeatures.class);
            if (fs != null) {
                for (FnFeature fnFeature : fs.value()) {
                    enableFeature(runtimeContext, fnFeature);
                }
            }

            FunctionConfigurer functionConfigurer = new FunctionConfigurer();
            functionConfigurer.configure(runtimeContext);


            codec.runCodec((evt) -> {
                try {
                    FunctionInvocationContext fic = runtimeContext.newInvocationContext(evt);
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

                        return output.withHeaders(output.getHeaders().setHeaders(fic.getAdditionalResponseHeaders()));


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

    private void enableFeature(FunctionRuntimeContext runtimeContext, FnFeature f) {
        RuntimeFeature rf;
        try {
            Class<? extends RuntimeFeature> featureClass = f.value();
            rf = featureClass.newInstance();
        } catch (Exception e) {
            throw new FunctionInitializationException("Could not load feature class " + f.value().toString(), e);
        }

        try {
            rf.initialize(runtimeContext);
        } catch (Exception e) {
            throw new FunctionInitializationException("Exception while calling initialization on runtime feature " + f.value(), e);
        }
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
            sb.append("Caused by: ").append(t.toString());
        }

        for (StackTraceElement elem : t.getStackTrace()) {
            if (elem.getClassName().startsWith("com.fnproject.fn")) {
                break;
            }
            sb.append("\n    at ").append(elem.toString());
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

