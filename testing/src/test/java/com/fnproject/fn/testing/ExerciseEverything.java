package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.HttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
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
    public FlowFuture<String> completedValue(Flow fl) {
        return fl.completedValue("completed value");
    }

    @Test(2)
    @Test.Expect("supply")
    public FlowFuture<String> supply(Flow fl) {
        return fl.supply(() -> "supply");
    }

    @Test(3)
    public FlowFuture<Void> allOfWithCompletedValue(Flow fl) {
        return fl.allOf(
                fl.completedValue(1),
                fl.completedValue(2),
                fl.completedValue(3)
        );
    }

    @Test(4)
    public FlowFuture<Void> allOfWithSuppliedValue(Flow fl) {
        return fl.allOf(
                fl.supply(() -> 1),
                fl.supply(() -> 2),
                fl.supply(() -> 3)
        );
    }

    @Test(5)
    @Test.Expect("1")
    @Test.Expect("2")
    @Test.Expect("3")
    public FlowFuture<Object> anyOfWithCompletedValue(Flow fl) {
        return fl.anyOf(
                fl.completedValue("1"),
                fl.completedValue("2"),
                fl.completedValue("3")
        );
    }

    @Test(6)
    @Test.Expect("1")
    @Test.Expect("2")
    @Test.Expect("3")
    public FlowFuture<Object> anyOfWithSuppliedValue(Flow fl) {
        return fl.anyOf(
                fl.supply(() -> "1"),
                fl.supply(() -> "2"),
                fl.supply(() -> "3")
        );
    }

    @Test(7)
    @Test.Expect("test exception")
    public FlowFuture<Exception> completeWithAnException(Flow fl) {
        return fl.completedValue(new Exception("test exception"));
    }

    @Test(8)
    @Test.Catch({FlowCompletionException.class, MyException.class})
    public FlowFuture<String> supplyAnException(Flow fl) {
        return fl.supply(() -> {
            throw new MyException("test exception");
        });
    }

    public static class MyException extends RuntimeException {
        MyException(String m) {
            super(m);
        }
    }

    @Test(9)
    @Test.Expect("4")
    public FlowFuture<Integer> chainThenApply(Flow fl) {
        FlowFuture<Integer> cf = fl.completedValue(0);

        for (int i = 0; i < 4; i++) {
            cf = cf.thenApply((x) -> x + 1);
        }
        return cf;
    }


    @Test(10)
    @Test.Expect("-3")
    public FlowFuture<Integer> catchBubbledException(Flow fl) {
        return fl.completedValue(0)
                .thenApply((x) -> x + 1)
                .thenApply((x) -> {
                    if (x == 1) throw new MyException("boom");
                    else return x + 1;
                })
                .thenApply((x) -> x + 1)
                .thenApply((x) -> x + 1)
                .exceptionally((e) -> -3);
    }

    @Test(11)
    @Test.Catch({FlowCompletionException.class, FunctionInvocationException.class})
    public FlowFuture<HttpResponse> nonexistentExternalEvaluation(Flow fl) {
        return fl.invokeFunction("nonexistent/nonexistent", HttpMethod.POST, Headers.emptyHeaders(), new byte[0]);
    }

    @Test(12)
    @Test.Expect("okay")
    public FlowFuture<String> checkPassingExternalInvocation(Flow fl) {
        return fl.invokeFunction(inputEvent.getAppName() + inputEvent.getPath(), HttpMethod.POST, Headers.emptyHeaders(), "PASS".getBytes())
                .thenApply((resp) -> {
                    return resp.getStatusCode() != 200 ? "failure" : new String(resp.getBodyAsBytes());
                });
    }

    // There is currently no way for a hot function to signal failure in the Fn platform.
    // This test will only work in default mode.
    @Test(13)
    @Test.Catch({FlowCompletionException.class, FunctionInvocationException.class})
    public FlowFuture<HttpResponse> checkFailingExternalInvocation(Flow fl) {
        return fl.invokeFunction(inputEvent.getAppName() + inputEvent.getPath(), HttpMethod.POST, Headers.emptyHeaders(), "FAIL".getBytes());
    }

    // This original version captures the RT, which captures the factory, which is not serializable
    @Test(14)
    @Test.Expect("X")
    public FlowFuture<String> simpleThenCompose(Flow fl) {
        return fl.completedValue("x").thenCompose((s) -> {
            System.err.println("I am in the thenCompose stage, s = " + s);
            FlowFuture<String> retVal = fl.completedValue(s.toUpperCase());
            System.err.println("my retVal = " + retVal + "; type is " + retVal.getClass());
            return retVal;
        });
    }

    @Test(15)
    @Test.Expect("hello world")
    public FlowFuture<String> thenCompose(Flow fl) {
        return fl.completedValue("hello")
                .thenCompose((s) ->
                        fl.supply(() -> s)
                                .thenApply((s2) -> s2 + " world")
                );
    }

    @Test(16)
    @Test.Expect("foo")
    public FlowFuture<String> thenComposeThenError(Flow fl) {
        return fl.completedValue("hello")
                .thenCompose((s) -> fl.supply(() -> {
                    if (s.equals("hello")) throw new MyException("foo");
                    else return s;
                }))
                .exceptionally(Throwable::getMessage);
    }

    @Test(17)
    @Test.Expect("foo")
    public FlowFuture<String> thenComposeWithErrorInBody(Flow fl) {
        return fl.completedValue("hello")
                .thenCompose((s) -> {
                    if (s.equals("hello")) throw new MyException("foo");
                    else return fl.completedValue(s);
                })
                .exceptionally(Throwable::getMessage);
    }

    @Test(18)
    @Test.Expect("a")
    @Test.Expect("b")
    public FlowFuture<String> applyToEither(Flow fl) {
        return fl.completedValue("a").applyToEither(fl.completedValue("b"), (x) -> x);
    }

    @Test(19)
    @Test.Expect("a")
    @Test.Expect("b")
    public FlowFuture<String> applyToEitherLikelyPathB(Flow fl) {
        return fl.supply(() -> "a").applyToEither(fl.completedValue("b"), (x) -> x);
    }

    @Test(20)
    public FlowFuture<Void> harmlessAcceptBoth(Flow fl) {
        return fl.completedValue("a")
                .thenAcceptBoth(
                        fl.completedValue("b"),
                        (a, b) -> System.err.println(a + "; " + b)
                );
    }

    @Test(21)
    @Test.Catch({FlowCompletionException.class, MyException.class})
    @Test.Expect("ab")
    public FlowFuture<Void> acceptBoth(Flow fl) {
        return fl.completedValue("a")
                .thenAcceptBoth(
                        fl.completedValue("b"),
                        (a, b) -> {
                            System.err.println("A is " + a + " and B is " + b);
                            throw new MyException(a + b);
                        });
    }

    @Test(22)
    @Test.Catch({FlowCompletionException.class, MyException.class})
    @Test.Expect("a")
    @Test.Expect("b")
    public FlowFuture<Void> acceptEither(Flow fl) {
        return fl.completedValue("a")
                .acceptEither(
                        fl.completedValue("b"),
                        (x) -> {
                            throw new MyException(x);
                        }
                );
    }

    @Test(23)
    @Test.Expect("foobar")
    public FlowFuture<String> thenCombine(Flow fl) {
        return fl.completedValue("foo")
                .thenCombine(fl.completedValue("bar"),
                        (a, b) -> a + b);
    }

    @Test(24)
    @Test.Expect("foo")
    public FlowFuture<String> thenCombineE1(Flow fl) {
        return fl.supply(() -> {
            throw new MyException("foo");
        })
                .thenCombine(fl.completedValue("bar"),
                        (a, b) -> a + b)
                .exceptionally(Throwable::getMessage);
    }

    @Test(25)
    @Test.Expect("bar")
    public FlowFuture<String> thenCombineE2(Flow fl) {
        return fl.completedValue("foo")
                .thenCombine(fl.supply(() -> {
                            throw new MyException("bar");
                        }),
                        (a, b) -> a + b)
                .exceptionally(Throwable::getMessage);
    }


    @Test(26)
    @Test.Expect("foobar")
    public FlowFuture<String> thenCombineE3(Flow fl) {
        return fl.completedValue("foo")
                .thenCombine(fl.completedValue("bar"),
                        (a, b) -> {
                            if (!a.equals(b)) throw new MyException(a + b);
                            else return "baz";
                        })
                .exceptionally(Throwable::getMessage);
    }

    @Test(27)
    @Test.Expect("foo")
    public FlowFuture<String> handleNoError(Flow fl) {
        return fl.completedValue("foo")
                .handle((v, e) -> v);
    }

    @Test(28)
    @Test.Expect("bar")
    public FlowFuture<String> handleWithError(Flow fl) {
        return fl.supply(() -> {
            throw new MyException("bar");
        })
                .handle((v, e) -> e.getMessage());
    }

    @Test(29)
    @Test.Expect("foo")
    public FlowFuture<String> whenCompleteNoError(Flow fl) {
        return fl.completedValue("foo")
                .whenComplete((v, e) -> {
                    System.err.println("In whenComplete, v=" + v);
                    throw new MyException(v);
                })
                .exceptionally(t -> {
                    // Should *not* get called.
                    System.err.println("In whenComplete.exceptionally, t=" + t);
                    return t.getMessage() + "bar";
                });
    }

    @Test(30)
    @Test.Expect("barbaz")
    public FlowFuture<String> whenCompleteWithError(Flow fl) {
        return fl.supply(() -> {
            if (true) throw new MyException("bar");
            else return "";
        })
                .whenComplete((v, e) -> {
                    System.err.println("In whenComplete, e=" + e);
                    throw new MyException(e.getMessage());
                })
                .exceptionally(t -> {
                    System.err.println("In whenComplete (with error) exceptionally , t=" + t);
                    return t.getMessage() + "baz";
                });
    }

    @Test(31)
    @Test.Expect("foobar")
    public FlowFuture<String> externallyCompletable(Flow fl) throws IOException {
        ExternalFlowFuture<HttpRequest> cf = fl.createExternalFuture();
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
    @Test.Expect("bar")
    public FlowFuture<HttpRequest> externallyCompletableDirectGet(Flow fl) throws IOException {
        ExternalFlowFuture<HttpRequest> cf = fl.createExternalFuture();
        HttpClient httpClient = new HttpClient();
        httpClient.execute(HttpClient
                .preparePost(cf.completionUrl().toString())
                .withHeader("My-Header", "foo")
                .withHeader("FnProject-Method", "post")
                .withBody("bar".getBytes()));
        return cf;
    }


    @Test(33)
    @Test.Catch({FlowCompletionException.class, ExternalCompletionException.class})
    @Test.Expect("External completion failed")
    public FlowFuture<HttpRequest> externalFutureFailureAndGet(Flow fl) throws IOException {
        ExternalFlowFuture<HttpRequest> cf = fl.createExternalFuture();
        HttpClient httpClient = new HttpClient();
        httpClient.execute(HttpClient
                .preparePost(cf.failUrl().toString())
                .withHeader("My-Header", "foo")
                .withHeader("FnProject-Method", "post")
                .withBody("bar".getBytes()));
        return cf;
    }


    @Test(34)
    @Test.Expect("foobar")
    public FlowFuture<String> externallyCompletableFailure(Flow fl) throws IOException {
        ExternalFlowFuture<HttpRequest> cf = fl.createExternalFuture();
        HttpClient httpClient = new HttpClient();
        httpClient.execute(HttpClient
                .preparePost(cf.failUrl().toString())
                .withHeader("My-Header", "foo")
                .withHeader("FnProject-Method", "post")
                .withBody("bar".getBytes()));
        return cf.thenApply((req) -> {
            System.err.println("got here");
            return "failed";
        })
                .exceptionally(e -> {
                            System.err.println("Got into exception with e=" + e);
                            return ((ExternalCompletionException) e).getExternalRequest().getHeaders().get("my-header").get() +
                                    new String(((ExternalCompletionException) e).getExternalRequest().getBodyAsBytes());
                        }
                );
    }


    @Test(35)
    @Test.Expect("foobar")
    public FlowFuture<String> exceptionallyComposeHandle(Flow fl) throws IOException {

        return fl.<String>failedFuture(new RuntimeException("foobar"))
                .exceptionallyCompose((e) -> fl.completedValue(e.getMessage()));
    }

    @Test(36)
    @Test.Expect("foobar")
    public FlowFuture<String> exceptionallyComposePassThru(Flow fl) throws IOException {

        return fl.completedValue("foobar")
                .exceptionallyCompose((e) -> fl.completedValue(e.getMessage()));
    }


    @Test(37)
    @Test.Expect("foobar")
    public FlowFuture<String> exceptionallyComposePropagateError(Flow fl) throws IOException {
        return fl.<String>failedFuture(new RuntimeException("foo"))
                .exceptionallyCompose((e) -> {
                    throw new RuntimeException("foobar");
                }).exceptionally(Throwable::getMessage);
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
                return IOUtils.toString(is, "utf-8");
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
        Flow fl = Flows.currentFlow();

        out.println("In main function");
        Map<Integer, FlowFuture<Object>> awaiting = new TreeMap<>();

        for (Map.Entry<Integer, Method> e : findTests(this).entrySet()) {
            id = e.getKey();
            Method m = e.getValue();

            out.println("Running test " + id);

            Test.Catch exWanted = m.getAnnotation(Test.Catch.class);
            String[] values = expectedValues(m);

            try {
                awaiting.put(id, (FlowFuture<Object>) m.invoke(this, fl));
            } catch (InvocationTargetException ex) {
                out.println("Failure setting up test " + id + ": " + ex.getCause());
                ex.printStackTrace(out);
                fail();
            } catch (IllegalAccessException e1) {
            }
        }

        for (Map.Entry<Integer, Method> e : findTests(this).entrySet()) {
            id = e.getKey();
            Method m = e.getValue();

            out.println("Running test " + id);

            Test.Catch exWanted = m.getAnnotation(Test.Catch.class);
            String[] values = expectedValues(m);
            try {
                FlowFuture<Object> cf = awaiting.get(id);
                if (cf == null) {
                    continue;
                }
                Object r = cf.get();

                // Coerce returned value to string
                String rv = coerceToString(r);

                if (!huntForValues(rv, values)) {
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

                    for (Class<?> c : exWanted.value()) {
                        if (t == null) {
                            out.println("  end of exception chain, wanted " + c);
                            fail();
                            break;
                        }
                        if (c.isAssignableFrom(t.getClass())) {
                            out.println("  exception type as wanted: " + t);
                            String message = coerceToString(t);

                            found = found || huntForValues(message, values);
                        } else {
                            out.println("  exception type mismatch: " + t + ", wanted " + c);
                            out.println("  Class loaders: " + t.getClass().getClassLoader() + ", wanted " + c.getClassLoader());
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
        } else if (r instanceof HttpRequest) {
            return new String(((HttpRequest) r).getBodyAsBytes());
        } else if (r instanceof HttpResponse) {
            return new String(((HttpResponse) r).getBodyAsBytes());
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

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Test {
        int value();

        @Retention(RetentionPolicy.RUNTIME)
        @interface Expected {
            Expect[] value();
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Repeatable(Expected.class)
        @interface Expect {
            String value();
        }

        @Retention(RetentionPolicy.RUNTIME)
        @interface Catch {
            Class<?>[] value();
        }
    }
}
