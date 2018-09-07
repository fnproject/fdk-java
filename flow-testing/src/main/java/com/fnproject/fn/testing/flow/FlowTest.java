package com.fnproject.fn.testing.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.*;
import com.fnproject.fn.testing.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * * Created on 07/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FlowTest implements FnTestingRuleFeature {
    private Map<String, FnFunctionStub> functionStubs = new HashMap<>();
    public static InMemCompleter completer = null;
    FnTestingRule rule;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public FlowTest(FnTestingRule rule) {

        this.rule = rule;
        rule.addSharedClass(InMemCompleter.CompleterInvokeClient.class);
        rule.addSharedClass(BlobStoreClient.class);
        rule.addSharedClass(BlobResponse.class);
        rule.addSharedClass(CompleterClientFactory.class);
        rule.addSharedClass(CompletionId.class);
        rule.addSharedClass(FlowId.class);
        rule.addSharedClass(Flow.FlowState.class);
        rule.addSharedClass(CodeLocation.class);
        rule.addSharedClass(HttpMethod.class);
        rule.addSharedClass(com.fnproject.fn.api.flow.HttpRequest.class);
        rule.addSharedClass(com.fnproject.fn.api.flow.HttpResponse.class);
        rule.addSharedClass(FlowCompletionException.class);
        rule.addSharedClass(FunctionInvocationException.class);
        rule.addSharedClass(PlatformException.class);
        rule.addFeature(this);
    }

    @Override
    public void prepareTest(ClassLoader functionClassLoader, PrintStream stderr, String cls, String method) {
        InMemCompleter.CompleterInvokeClient client = new TestRuleCompleterInvokeClient(functionClassLoader, stderr, cls, method);
        InMemCompleter.FnInvokeClient fnInvokeClient = new TestRuleFnInvokeClient();

        // The following must be a static: otherwise the factory (the lambda) will not be serializable.
        completer = new InMemCompleter(client, fnInvokeClient);

    }

    @Override
    public void prepareFunctionClassLoader(FnTestingClassLoader cl) {

    }

    @Override
    public void afterTestComplete() {
        completer.awaitTermination();
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
        public APIModel.CompletionResult invokeStage(String fnId, FlowId flowId, CompletionId stageId, APIModel.Blob closure, List<APIModel.CompletionResult> input) {
            // Construct a new ClassLoader hierarchy with a copy of the statics embedded in the runtime.
            // Initialise it appropriately.
            FnTestingClassLoader fcl = new FnTestingClassLoader(functionClassLoader, rule.getSharedPrefixes());


            setCompleterClient(fcl, completer);


            APIModel.InvokeStageRequest request = new APIModel.InvokeStageRequest();
            request.stageId = stageId.getId();
            request.flowId = flowId.getId();
            request.closure = closure;
            request.args = input;

            String inputBody = null;
            try {
                inputBody = objectMapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Invalid request");
            }

            // oldSystemErr.println("Body\n" + new String(inputBody));

            InputStream is = new FnHttpEventBuilder()
              .withBody(inputBody)
              .withAppName("appName")
              .withRoute("/route").withRequestUrl("http://some/url")
              .withMethod("POST")
              .withHeader("Content-Type", "application/json")
              .withHeader(FlowContinuationInvoker.FLOW_ID_HEADER, flowId.getId()).currentEventInputStream();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Map<String, String> mutableEnv = new HashMap<>();
            PrintStream functionOut = new PrintStream(output);
            PrintStream functionErr = new PrintStream(oldSystemErr);

            // Do we want to capture IO from continuations on the main log stream?
            // System.setOut(functionErr);
            // System.setErr(functionErr);

            mutableEnv.putAll(rule.getConfig());
            mutableEnv.putAll(rule.getEventEnv());
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
            APIModel.CompletionResult r;
            try {
                // Read wrapping result, and throw it away
                parser.parse();
                IdentityInputStream iis = new IdentityInputStream(sib);
                byte[] responseBody = IOUtils.toByteArray(iis);

                APIModel.InvokeStageResponse response = objectMapper.readValue(responseBody, APIModel.InvokeStageResponse.class);
                r = response.result;

            } catch (Exception e) {
                oldSystemErr.println("Err\n" + e);
                e.printStackTrace(oldSystemErr);
                r = APIModel.CompletionResult.failure(APIModel.ErrorDatum.newError(APIModel.ErrorType.UnknownError, "Error reading fn Response:" + e.getMessage()));
            }

            if (!r.successful) {
                throw new ResultException(r.result);
            }
            return r;

        }
    }

    private void setCompleterClient(FnTestingClassLoader cl, CompleterClientFactory completerClientFactory) {
        try {
            Class<?> completerGlobals = cl.loadClass(FlowRuntimeGlobals.class.getName());
            completerGlobals.getMethod("setCompleterClientFactory", CompleterClientFactory.class).invoke(completerGlobals, completerClientFactory);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        }
    }

    private interface FnFunctionStub {
        com.fnproject.fn.api.flow.HttpResponse stubFunction(HttpMethod method, Headers headers, byte[] body);
    }


    public FnFunctionStubBuilder<FlowTest> givenFn(String id) {
        return new FnFunctionStubBuilder<FlowTest>() {
            @Override
            public FlowTest withResult(byte[] result) {
                return withAction((body) -> result);
            }

            @Override
            public FlowTest withFunctionError() {
                return withAction((body) -> {
                    throw new FunctionError("simulated by testing platform");
                });
            }

            @Override
            public FlowTest withPlatformError() {
                return withAction((body) -> {
                    throw new PlatformError("simulated by testing platform");
                });
            }

            @Override
            public FlowTest withAction(ExternalFunctionAction f) {
                functionStubs.put(id, (HttpMethod method, Headers headers, byte[] body) -> {
                    try {
                        return new DefaultHttpResponse(200, Headers.emptyHeaders(), f.apply(body));
                    } catch (FunctionError functionError) {
                        return new DefaultHttpResponse(500, Headers.emptyHeaders(), functionError.getMessage().getBytes());
                    } catch (PlatformError platformError) {
                        throw new RuntimeException("Platform Error");
                    }
                });
                return FlowTest.this;
            }
        };
    }

    private class TestRuleFnInvokeClient implements InMemCompleter.FnInvokeClient {
        @Override
        public CompletableFuture<HttpResponse> invokeFunction(String fnId, HttpMethod method, Headers headers, byte[] data) {
            FnFunctionStub stub = functionStubs
              .computeIfAbsent(fnId, (k) -> {
                  throw new IllegalStateException("Function was invoked that had no definition: " + k);
              });

            try {
                return CompletableFuture.completedFuture(stub.stubFunction(method, headers, data));
            } catch (Exception e) {
                CompletableFuture<com.fnproject.fn.api.flow.HttpResponse> respFuture = new CompletableFuture<>();
                respFuture.completeExceptionally(e);
                return respFuture;
            }
        }
    }
}
