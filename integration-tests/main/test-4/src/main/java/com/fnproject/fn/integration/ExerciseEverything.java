package com.fnproject.fn.integration;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.cloudthreads.*;
import com.fnproject.fn.runtime.cloudthreads.HttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ExerciseEverything {

    private boolean okay = true;
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(new TeeOutputStream(System.err, bos));
    private String testSelector = null;
    private InputEvent inputEvent;
    private List<Integer> failures = new ArrayList<>();

    @Test(1)
    @Test.Expect("completed value")
    public CloudFuture<String> completedValue(CloudThreadRuntime rt) {
        return rt.completedValue("completed value");
    }

    @Test(2)
    @Test.Expect("supply")
    public CloudFuture<String> supply(CloudThreadRuntime rt) {
        return rt.supply(() -> "supply");
    }

    @Test(3)
    public CloudFuture<Void> allOfWithCompletedValue(CloudThreadRuntime rt) {
        return rt.allOf(
                rt.completedValue(1),
                rt.completedValue(2),
                rt.completedValue(3)
        );
    }

    @Test(4)
    public CloudFuture<Void> allOfWithSuppliedValue(CloudThreadRuntime rt) {
        return rt.allOf(
                rt.supply(() -> 1),
                rt.supply(() -> 2),
                rt.supply(() -> 3)
        );
    }

    @Test(5)
    @Test.Expect("1")
    @Test.Expect("2")
    @Test.Expect("3")
    public CloudFuture<Object> anyOfWithCompletedValue(CloudThreadRuntime rt) {
        return rt.anyOf(
                rt.completedValue("1"),
                rt.completedValue("2"),
                rt.completedValue("3")
        );
    }

    @Test(6)
    @Test.Expect("1")
    @Test.Expect("2")
    @Test.Expect("3")
    public CloudFuture<Object> anyOfWithSuppliedValue(CloudThreadRuntime rt) {
        return rt.anyOf(
                rt.supply(() -> "1"),
                rt.supply(() -> "2"),
                rt.supply(() -> "3")
        );
    }

    @Test(7)
    @Test.Expect("test exception")
    public CloudFuture<Exception> completeWithAnException(CloudThreadRuntime rt) {
        return rt.completedValue(new Exception("test exception"));
    }

    @Test(8)
    @Test.Catch({CloudCompletionException.class, MyException.class})
    public CloudFuture<String> supplyAnException(CloudThreadRuntime rt) {
        return rt.supply(() -> { throw new MyException("test exception"); });
    }

    public static class MyException extends RuntimeException {
        MyException(String m) { super(m); }
    }

    @Test(9)
    @Test.Expect("4")
    public CloudFuture<Integer> chainThenApply(CloudThreadRuntime rt) {
        CloudFuture<Integer> cf = rt.completedValue(0);

        for (int i = 0; i < 4; i++) {
            cf = cf.thenApply((x) -> x + 1);
        }
        return cf;
    }


    @Test(10)
    @Test.Expect("-3")
    public CloudFuture<Integer> catchBubbledException(CloudThreadRuntime rt) {
        return rt.completedValue(0)
                .thenApply((x) -> x + 1)
                .thenApply((x) -> { if (x == 1) throw new MyException("boom"); else return x + 1; })
                .thenApply((x) -> x + 1)
                .thenApply((x) -> x + 1)
                .exceptionally((e) -> -3);
    }

    @Test(11)
    @Test.Catch(FunctionInvocationException.class)
    public CloudFuture<HttpResponse> nonexistentExternalEvaluation(CloudThreadRuntime rt) {
        return rt.invokeFunction("nonexistent", HttpMethod.POST, Headers.emptyHeaders(), new byte[0]);
    }

    @Test(12)
    @Test.Expect("okay")
    public CloudFuture<String> checkPassingExternalInvocation(CloudThreadRuntime rt) {
        return rt.invokeFunction(inputEvent.getAppName() + inputEvent.getRoute(), HttpMethod.POST, Headers.emptyHeaders(), "PASS".getBytes())
                .thenApply((resp) -> {
                    return resp.getStatusCode() != 200 ? "failure" : new String(resp.getBodyAsBytes());
                });
    }

    // There is currently no way for a hot function to signal failure in the Fn platform.
    // This test will only work in default mode.
    @Test(13)
    @Test.Catch(FunctionInvocationException.class)
    public CloudFuture<HttpResponse> checkFailingExternalInvocation(CloudThreadRuntime rt) {
        return rt.invokeFunction(inputEvent.getAppName() + inputEvent.getRoute(), HttpMethod.POST, Headers.emptyHeaders(), "FAIL".getBytes());
    }

    @Test(14)
    @Test.Expect("X")
    public CloudFuture<String> simpleThenCompose(CloudThreadRuntime rt) {
        return rt.completedValue("x").thenCompose((s) -> {
            System.err.println("I am in the thenCompose stage, s = " + s);
            CloudFuture<String> retVal = rt.completedValue(s.toUpperCase());
            System.err.println("my retVal = " + retVal + "; type is " + retVal.getClass());
            return retVal;
        });
    }

    @Test(15)
    @Test.Expect("hello world")
    public CloudFuture<String> thenCompose(CloudThreadRuntime rt) {
        return rt.completedValue("hello")
                 .thenCompose((s) ->
                        rt.supply(() -> s)
                          .thenApply((s2) -> s2 + " world")
                 );
    }

    @Test(16)
    @Test.Expect("foo")
    public CloudFuture<String> thenComposeThenError(CloudThreadRuntime rt) {
        return rt.completedValue("hello")
                .thenCompose((s) -> rt.supply(() -> { if (s.equals("hello")) throw new MyException("foo"); else return s; }))
                .exceptionally(Throwable::getMessage);
    }

    @Test(17)
    @Test.Expect("foo")
    public CloudFuture<String> thenComposeWithErrorInBody(CloudThreadRuntime rt) {
        return rt.completedValue("hello")
                .thenCompose((s) -> { if (s.equals("hello")) throw new MyException("foo"); else return rt.completedValue(s); })
                .exceptionally(Throwable::getMessage);
    }

    @Test(18)
    @Test.Expect("a")
    @Test.Expect("b")
    public CloudFuture<String> applyToEither(CloudThreadRuntime rt) {
        return rt.completedValue("a").applyToEither(rt.completedValue("b"), (x) -> x);
    }

    @Test(19)
    @Test.Expect("a")
    @Test.Expect("b")
    public CloudFuture<String> applyToEitherLikelyPathB(CloudThreadRuntime rt) {
        return rt.supply(() -> "a").applyToEither(rt.completedValue("b"), (x) -> x);
    }

    @Test(20)
    public CloudFuture<Void> harmlessAcceptBoth(CloudThreadRuntime rt) {
        return rt.completedValue("a")
                .thenAcceptBoth(
                    rt.completedValue("b"),
                        (a, b) -> System.err.println(a + "; " + b)
                );
    }

    @Test(21)
    @Test.Catch({CloudCompletionException.class, MyException.class})
    @Test.Expect("ab")
    public CloudFuture<Void> acceptBoth(CloudThreadRuntime rt) {
        return rt.completedValue("a")
                .thenAcceptBoth(
                    rt.completedValue("b"),
                        (a, b) -> {
                            System.err.println("A is " + a + " and B is " + b);
                            throw new MyException(a + b);
                        });
    }

    @Test(22)
    @Test.Catch({CloudCompletionException.class, MyException.class})
    @Test.Expect("a")
    @Test.Expect("b")
    public CloudFuture<Void> acceptEither(CloudThreadRuntime rt) {
        return rt.completedValue("a")
                .acceptEither(
                        rt.completedValue("b"),
                            (x) -> { throw new MyException(x); }
                );
    }

    @Test(23)
    @Test.Expect("foobar")
    public CloudFuture<String> thenCombine(CloudThreadRuntime rt) {
        return rt.completedValue("foo")
                .thenCombine(rt.completedValue("bar"),
                        (a, b) -> a + b);
    }

    @Test(24)
    @Test.Expect("foo")
    public CloudFuture<String> thenCombineE1(CloudThreadRuntime rt) {
        return rt.supply(() -> { throw new MyException("foo"); })
                .thenCombine(rt.completedValue("bar"),
                        (a, b) -> a + b)
                .exceptionally(Throwable::getMessage);
    }

    @Test(25)
    @Test.Expect("bar")
    public CloudFuture<String> thenCombineE2(CloudThreadRuntime rt) {
        return rt.completedValue("foo")
                .thenCombine(rt.supply(() -> { throw new MyException("bar"); }),
                        (a, b) -> a + b)
                .exceptionally(Throwable::getMessage);
    }


    @Test(26)
    @Test.Expect("foobar")
    public CloudFuture<String> thenCombineE3(CloudThreadRuntime rt) {
        return rt.completedValue("foo")
                .thenCombine(rt.completedValue("bar"),
                        (a, b) -> { if (! a.equals(b)) throw new MyException(a + b); else return "baz"; })
                .exceptionally(Throwable::getMessage);
    }

    @Test(27)
    @Test.Expect("foo")
    public CloudFuture<String> handleNoError(CloudThreadRuntime rt) {
        return rt.completedValue("foo")
                .handle((v, e) -> v);
    }

    @Test(28)
    @Test.Expect("bar")
    public CloudFuture<String> handleWithError(CloudThreadRuntime rt) {
        return rt.supply(() -> { throw new MyException("bar"); })
                .handle((v, e) -> e.getMessage());
    }

    @Test(29)
    @Test.Expect("foo")
    public CloudFuture<String> whenCompleteNoError(CloudThreadRuntime rt) {
        return rt.completedValue("foo")
                .whenComplete((v, e) -> { throw new MyException(v); })
                .exceptionally(Throwable::getMessage);
    }

    @Test(30)
    @Test.Expect("bar")
    public CloudFuture<String> whenCompleteWithError(CloudThreadRuntime rt) {
        return rt.supply(() -> { if (true) throw new MyException("bar"); else return ""; })
                .whenComplete((v, e) -> { throw new MyException(e.getMessage()); })
                .exceptionally(Throwable::getMessage);
    }

    @Test(31)
    @Test.Expect("foobar")
    public CloudFuture<String> externallyCompletable(CloudThreadRuntime rt) throws IOException {
        ExternalCloudFuture<HttpRequest> cf = rt.createExternalFuture();
        HttpClient httpClient = new HttpClient();
        httpClient.execute(httpClient
                .preparePost(cf.completionUrl().toString())
                .withHeader("My-Header", "foo")
                .withHeader("FnProject-Method", "post")
                .withBody("bar".getBytes()));
        return cf.thenApply((req) ->
                req.getHeaders().get("my-header").get() + new String(req.getBodyAsBytes())
        );
    }

    @Test(32)
    @Test.Expect("foobar")
    public CloudFuture<String> externallyCompletableFailure(CloudThreadRuntime rt) throws IOException {
        ExternalCloudFuture<HttpRequest> cf = rt.createExternalFuture();
        HttpClient httpClient = new HttpClient();
        httpClient.execute(httpClient
                .preparePost(cf.failUrl().toString())
                .withHeader("My-Header", "foo")
                .withHeader("FnProject-Method", "post")
                .withBody("bar".getBytes()));
        return cf.thenApply((req) -> "failed")
                .exceptionally(e ->
                        ((ExternalCompletionException)e).getExternalRequest().getHeaders().get("my-header").get() +
                        new String(((ExternalCompletionException)e).getExternalRequest().getBodyAsBytes())
        );
    }

    private int id;

    void fail() {
        if (!failures.contains(id)) {
            failures.add(id);
        }
        okay = false;
    }

    public String handleRequest(InputEvent ie) {
        this.inputEvent = ie;
        String selector = ie.consumeBody((InputStream is) -> {
            try {
                return IOUtils.toString(is,"utf-8");
            } catch (IOException e) {
                return "FAIL";
            }
        });

        if ("PASS".equals(selector)) {
            return "okay";
        } else if ("FAIL".equals(selector)) {
            throw new MyException("failure demanded");
        }
        testSelector = selector;
        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        out.println("In main function");
        for (Map.Entry<Integer, Method> e: findTests(this).entrySet()) {
            id = e.getKey();
            Method m = e.getValue();

            out.println("Running test " + id);

            Test.Catch exWanted = m.getAnnotation(Test.Catch.class);
            String[] values = expectedValues(m);

            try {

                CloudFuture<Object> cf = (CloudFuture<Object>) m.invoke(this, rt);
                Object r = cf.get();

                boolean found = false;

                // Coerce returned value to string
                String rv = coerceToString(r);

                if (! huntForValues(rv, values)) {
                    fail();
                }

                if (exWanted != null) {
                    out.println("  expecting throw of " + Arrays.toString(exWanted.value()));
                    fail();
                }

            } catch (Throwable t) {
                if (exWanted != null) {
                    // We have a series of wrapped exceptions that should follow this containment pattern
                    boolean found = false;

                    for (Class<?> c: exWanted.value()) {
                        if (t == null) {
                            out.println("  end of exception chain, wanted " + c);
                            fail();
                            break;
                        } if (c.isAssignableFrom(t.getClass())) {
                            out.println("  exception type as wanted: " + t);
                            String message = coerceToString(t);

                            found = found || huntForValues(message, values);
                        } else {
                            out.println("  exception type mismatch: " + t + ", wanted " + c);
                            t.printStackTrace(out);
                            fail();
                            break;
                        }
                        t = t.getCause();
                    }
                    if (!found && values.length > 0) {
                        out.println("  failed comparison, wanted exception with one of " + Arrays.toString(values));
                        fail();
                    }
                } else {
                    out.println("  got an unexpected exception: " + t);
                    t.printStackTrace(out);
                    fail();
                }
            }
        }

        out.println(okay ? "Everything worked" : "There were failures: " + failures);
        out.flush();
        return bos.toString();
    }

    String coerceToString(Object r) {
        if (r == null) {
            return null;
        } else if (r instanceof String) {
            // okay
            return (String) r;
        } else if (r instanceof Throwable) {
            return ((Throwable) r).getMessage();
        } else {
            return r.toString();
        }
    }

    String[] expectedValues(Method m) {
        Test.Expected ex = m.getAnnotation(Test.Expected.class);
        if (ex != null) {
            return Arrays.stream(ex.value()).map(Test.Expect::value).toArray(String[]::new);
        } else {
            Test.Expect ex2 = m.getAnnotation(Test.Expect.class);
            if (ex2 != null) {
                return new String[]{ex2.value()};
            } else {
                return new String[]{};
            }
        }
    }

    boolean huntForValues(String match, String... values) {
        for (String v : values) {
            if ((v == null && match == null) || (v != null && v.equals(match))) {
                out.println("  successfully = " + match);
                return true;
            }
        }
        if (values.length > 0) {
            out.println("  failed comparison, wanted one of " + Arrays.toString(values) + " but got " + match);
            return false;
        }
        return true;
    }

    Map<Integer, Method> findTests(Object target) {
        Map<Integer, Method> tests = new TreeMap<>();
        for (Method m : target.getClass().getMethods()) {
            Test ann = m.getAnnotation(Test.class);
            if (ann == null)
                continue;
            int id = ann.value();
            tests.put(id, m);
        }
        if (testSelector == null || testSelector.trim().isEmpty()) {
            return tests;
        }

        return Arrays.stream(testSelector.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .filter(tests::containsKey)
                .collect(Collectors.toMap((x) -> x, tests::get));
    }
}
