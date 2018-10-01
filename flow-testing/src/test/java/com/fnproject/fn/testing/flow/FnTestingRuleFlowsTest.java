package com.fnproject.fn.testing.flow;

import com.fnproject.fn.api.*;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.FlowFeature;
import com.fnproject.fn.testing.FnTestingRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;


public class FnTestingRuleFlowsTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    private FlowTesting flow =  FlowTesting.create(fn);

    @FnFeature(FlowFeature.class)
    public static class Loop {

        public static int COUNT = 5;

        public String repeat(String s) {
            Flow fl = Flows.currentFlow();

            return fl
                    .completedValue(new Triple<>(COUNT, s, ""))
                    .thenCompose(Loop::loop)
                    .get();
        }


        public static FlowFuture<String> loop(Triple<Integer, String, String> triple) {
            int i = triple.first;
            String s = triple.second;
            String acc = triple.third;
            Flow fl = Flows.currentFlow();

            if (i == 0) {
                return fl.completedValue(acc);
            } else {
                return fl.completedValue(triple(i - 1, s, acc + s))
                        .thenCompose(Loop::loop);
            }
        }

        public static Triple<Integer, String, String> triple(int i, String s1, String s2) {
            return new Triple<>(i, s1, s2);
        }

        public static class Triple<U, V, W> implements Serializable {
            public final U first;
            public final V second;
            public final W third;

            public Triple(U first, V second, W third) {
                this.first = first;
                this.second = second;
                this.third = third;
            }
        }
    }

    @Before
    public void setup() {
        fn.addSharedClass(FnTestingRuleFlowsTest.class);
        fn.addSharedClass(Result.class);

        reset();
    }

    @Test
    public void completedValue() {
        fn.givenEvent().enqueue();
        fn.thenRun(TestFn.class, "completedValue");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.CompletedValue);
    }

    @Test
    public void supply() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "supply");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Supply);
    }

    @Test
    public void allOf() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "allOf");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.AllOf);
    }


    @Test
    public void anyOf() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "anyOf");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.AnyOf);
    }

    @Test()
    public void nestedThenCompose() {
        fn.givenEvent()
                .withBody("hello world")
                .enqueue();

        fn.thenRun(Loop.class, "repeat");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(fn.getOnlyResult().getBodyAsString())
                  .isEqualTo(String.join("", Collections.nCopies(Loop.COUNT, "hello world")));
    }

    @Test
    public void invokeFunctionWithResult() {
        fn.givenEvent().enqueue();
        flow.givenFn("user/echo")
                .withResult(Result.InvokeFunctionFixed.name().getBytes());

        fn.thenRun(TestFn.class, "invokeFunctionEcho");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.InvokeFunctionFixed);
    }


    @Test
    public void invokeJsonFunction() {
        fn.givenEvent().enqueue();
        flow.givenFn("user/json")
                .withAction((ign) -> {
                    if (new String(ign).equals("{\"foo\":\"bar\"}")) {
                        return "{\"foo\":\"baz\"}".getBytes();
                    } else {
                        return new byte[0];
                    }
                });

        fn.thenRun(TestFn.class, "invokeJsonFunction");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    public void invokeFunctionWithFunctionError() {
        fn.givenEvent().enqueue();
        flow.givenFn("user/error")
                .withFunctionError();

        fn.thenRun(TestFn.class, "invokeFunctionError");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
        isInstanceOfAny(exception, FunctionInvocationException.class);
    }

    @Test
    public void invokeFunctionWithFailedFuture() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "failedFuture");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
        Assertions.assertThat(exception).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(exception).hasMessage("failedFuture");
    }

    @Test
    public void invokeFunctionWithPlatformError() {
        fn.givenEvent().enqueue();
        flow.givenFn("user/error")
                .withPlatformError();

        fn.thenRun(TestFn.class, "invokeFunctionError");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
        isInstanceOfAny(exception, PlatformException.class);
    }

    @Test
    public void invokeFunctionWithAction() {
        fn.givenEvent().enqueue();
        flow.givenFn("user/echo")
                .withAction((p) -> p);

        fn.thenRun(TestFn.class, "invokeFunctionEcho");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.InvokeFunctionEcho);
    }

    @Test
    public void completingExceptionally() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "completeExceptionally");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
    }

    @Test
    public void completingExceptionallyWhenErrorIsThrownEarlyInGraph() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "completeExceptionallyEarly");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
    }

    @Test
    public void cancelledFutureCompletesExceptionally() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "cancelFuture");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
        Assertions.assertThat(exception).isInstanceOf(CancellationException.class);
    }

    @Test
    public void completeFutureExceptionallyWithCustomException() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "completeFutureExceptionally");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.Exceptionally);
        Assertions.assertThat(exception).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("Custom exception");
    }

    @Test
    public void completedFutureCompletesNormally() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "completeFuture");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.CompletedValue);
    }

    @Test
    public void uncompletedFutureCanBeCompleted() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "createFlowFuture");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.CompletedValue);
    }

    @Test
    public void shouldLogMessagesToStdErrToPlatformStdErr() {
        // Questionable: for testing, do we point all stderr stuff to the same log stream?
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "logToStdErrInContinuation");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(fn.getStdErrAsString()).contains("TestFn logging: 1");
    }

    @Test
    public void shouldLogMessagesToStdOutToPlatformStdErr() {
        // Questionable: for testing, do we point all stderr stuff to the same log stream?
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "logToStdOutInContinuation");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(fn.getStdErrAsString()).contains("TestFn logging: 1");
    }


    @Test
    public void shouldHandleMultipleEventsForFunctionWithoutInput() {
        fn.givenEvent().enqueue(2);

        fn.thenRun(TestFn.class, "anyOf");

        Assertions.assertThat(fn.getResults().get(0).getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(fn.getResults().get(1).getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(result).isEqualTo(Result.AnyOf);
    }

    @Test
    public void exceptionallyComposeHandle() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "exceptionallyComposeHandle");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(count).isEqualTo(2);

    }


    @Test
    public void exceptionallyComposePassThru() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "exceptionallyComposePassThru");
        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(count).isEqualTo(1);

    }

    @Test
    public void exceptionallyComposeThrowsError() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "exceptionallyComposeThrowsError");
        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);
        Assertions.assertThat(count).isEqualTo(1);

    }

    @Test()
    public void shouldHandleMultipleEventsForFunctionWithInput() {
        String[] bodies = {"hello", "world", "test"};
        for (int i = 0; i < bodies.length; i++) {
            fn.givenEvent().withBody(bodies[i]).enqueue();
        }

        fn.thenRun(Loop.class, "repeat");

        for (int i = 0; i < bodies.length; i++) {
            Assertions.assertThat(fn.getResults().get(i).getBodyAsString())
                      .isEqualTo(String.join("", Collections.nCopies(Loop.COUNT, bodies[i])));
        }
    }

    @Test()
    public void shouldRunShutdownHooksInTest() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "terminationHooks");
        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(OutputEvent.Status.Success);

        Assertions.assertThat(result).isEqualTo(Result.TerminationHookRun);

    }

    // Due to the alien nature of the stored exception, we supply a helper to assert isInstanceOfAny
    void isInstanceOfAny(Object o, Class<?>... cs) {
        Assertions.assertThat(o).isNotNull();
        ClassLoader loader = o.getClass().getClassLoader();
        for (Class<?> c : cs) {
            try {
                if (loader.loadClass(c.getName()).isAssignableFrom(o.getClass())) {
                    return;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        Assert.fail("Object " + o + "is not an instance of any of " + Arrays.toString(cs));
    }

    @FnFeature(FlowFeature.class)
    public static class TestFn {
        static Integer TO_ADD = null;

        public TestFn(RuntimeContext ctx) {
            TO_ADD = Integer.parseInt(ctx.getConfigurationByKey("ADD").orElse("-1"));
        }


        public void completedValue() {
            Flows.currentFlow()
                    .completedValue(Result.CompletedValue).thenAccept((r) -> result = r);
        }

        public void supply() {
            Flows.currentFlow()
                    .supply(() -> {
                        return Result.Supply;
                    }).thenAccept((r) -> result = r);
        }

        public void allOf() {
            Flow fl = Flows.currentFlow();
            fl.allOf(
                    fl.completedValue(1),
                    fl.completedValue(-1)
            ).thenAccept((r) -> {
                result = Result.AllOf;
            });
        }

        public void anyOf() {
            Flow fl = Flows.currentFlow();
            fl.anyOf(
                    fl.completedValue(1),
                    fl.completedValue(-1)
            ).thenAccept((r) -> result = Result.AnyOf);
        }

        public void thenCompose() {
            Flow fl = Flows.currentFlow();
            fl.completedValue(1)
                    .thenCompose((x) ->
                            fl.completedValue(1)
                                    .thenApply((y) -> x + y)
                    )
                    .thenAccept((r) -> result = Result.AnyOf);

        }

        public void exceptionallyComposeHandle() {

            Flow fl = Flows.currentFlow();
            fl.<Integer>failedFuture(new RuntimeException("error"))
                    .exceptionallyCompose((e) -> {
                        if (count == 0 && e.getMessage().equals("error")) {
                            count = 1;
                            return fl.completedValue(1);
                        }
                        return fl.completedValue(0);
                    })
                    .thenAccept((i) -> {
                        if (count == 1 && i == 1) {
                            count++;
                        }
                    });
        }

        public void exceptionallyComposePassThru() {

            Flow fl = Flows.currentFlow();
            fl.completedValue(-1)
                    .exceptionallyCompose((e) -> {
                        count--;
                        return fl.completedValue(1);
                    })
                    .thenAccept((i) -> {
                        if (count == 0 && i == -1) {
                            count++;
                        }
                    });
        }

        public void exceptionallyComposeThrowsError() {

            Flow fl = Flows.currentFlow();
            fl.failedFuture(new RuntimeException("error"))
                    .exceptionallyCompose((e) -> {
                        if (e.getMessage().equals("error")) {
                            throw new RuntimeException("foo");
                        }
                        count--;
                        return fl.completedValue(-1);
                    })
                    .whenComplete((i, e) -> {
                        if (e != null && e.getMessage().equals("foo")) {
                            count++;
                        }
                    });
        }


        public void invokeFunctionEcho() {
            Flow fl = Flows.currentFlow();
            fl.invokeFunction("user/echo", HttpMethod.GET, Headers.emptyHeaders(), Result.InvokeFunctionEcho.name().getBytes())
                    .thenAccept((r) -> result = Result.valueOf(new String(r.getBodyAsBytes())));
        }

        public static class JSONObject implements  Serializable{
            public String foo = "bar";
        }

        public void invokeJsonFunction() {
            Flows.currentFlow()
                    .invokeFunction("user/json", new JSONObject(), JSONObject.class)
                    .thenAccept((json) -> {
                        if (json.foo.equals("baz")) {
                            count = 1;
                        }
                    });
        }

        public void invokeFunctionError() {
            Flow fl = Flows.currentFlow();
            fl.invokeFunction("user/error", HttpMethod.GET, Headers.emptyHeaders(), new byte[]{})
                    .exceptionally((e) -> {
                        result = Result.Exceptionally;
                        exception = e;
                        return null;
                    });
        }

        public void completeExceptionally() {
            Flow fl = Flows.currentFlow();
            fl.supply(() -> {
                throw new RuntimeException("This function should fail");
            })
                    .exceptionally((ex) -> result = Result.Exceptionally);
        }

        public void failedFuture() {
            Flow fl = Flows.currentFlow();
            fl.failedFuture(new RuntimeException("failedFuture"))
                    .exceptionally((ex) -> {
                        result = Result.Exceptionally;
                        exception = ex;
                        return null;
                    });
        }

        public void completeExceptionallyEarly() {
            Flow fl = Flows.currentFlow();
            fl.completedValue(null)
                    .thenApply((x) -> {
                        throw new RuntimeException("This function should fail");
                    })
                    .thenApply((x) -> 2)
                    .exceptionally((ex) -> {
                        result = Result.Exceptionally;
                        return null;
                    });
        }

        public void cancelFuture() {
            Flow fl = Flows.currentFlow();

            FlowFuture<Result> f = fl.supply(() -> {
                new CompletableFuture<>().get();
                return Result.Supply;
            });

            f.exceptionally((ex) -> {
                result = Result.Exceptionally;
                exception = ex;
                return Result.Exceptionally;
            });

            f.cancel();
        }

        public void completeFutureExceptionally() {
            Flow fl = Flows.currentFlow();

            FlowFuture<Result> f = fl.supply(() -> {
                new CompletableFuture<>().get();
                return Result.Supply;
            });

            f.exceptionally((ex) -> {
                result = Result.Exceptionally;
                exception = ex;
                return Result.Exceptionally;
            });

            f.completeExceptionally(new RuntimeException("Custom exception"));
        }

        public void completeFuture() {
            Flow fl = Flows.currentFlow();

            FlowFuture<Result> f = fl.supply(() -> {
                new CompletableFuture<>().get();
                return Result.Supply;
            });

            f.thenAccept((r) -> result = r);
            f.complete(Result.CompletedValue);
        }

        public void createFlowFuture() {
            Flow fl = Flows.currentFlow();

            FlowFuture<Result> f = fl.createFlowFuture();

            f.thenAccept((r) -> result = r);
            f.complete(Result.CompletedValue);
        }

        public void logToStdErrInContinuation() {
            Flow fl = Flows.currentFlow();
            fl.completedValue(1)
                    .thenApply((x) -> {
                        System.err.println("TestFn logging: " + x);
                        return x;
                    })
                    .thenApply((x) -> x + 1);
        }

        public void logToStdOutInContinuation() {
            Flow fl = Flows.currentFlow();
            fl.completedValue(1)
                    .thenApply((x) -> {
                        System.err.println("TestFn logging: " + x);
                        return x;
                    })
                    .thenApply((x) -> x + 1);
        }

        public void cannotReadConfigVarInContinuation() {
            Flow fl = Flows.currentFlow();
            TO_ADD = 3;
            fl.completedValue(1)
                    .thenAccept((x) -> {
                        staticConfig = TO_ADD;
                    });
        }

        public void terminationHooks() {
            Flow fl = Flows.currentFlow();

            fl.supply(() -> 1)
                    .thenAccept((i) -> {
                        System.err.println("Hello");
                    });

            fl.addTerminationHook((s) -> {
                if (staticConfig == 2) {
                    result = Result.TerminationHookRun;
                }

            });

            fl.addTerminationHook((s) -> {
                staticConfig++;
            });
            fl.addTerminationHook((s) -> {
                staticConfig = 1;
            });
        }

    }


    public enum Result {
        CompletedValue,
        Supply,
        AllOf,
        InvokeFunctionEcho,
        InvokeFunctionFixed,
        AnyOf, Exceptionally,
        ThenCompose,
        ThenComplete,
        TerminationHookRun
    }

    static void reset() {
        result = null;
        exception = null;
        staticConfig = null;
        count = 0;
    }

    // These members are external to the class under test so as to be visible from the unit tests.
    // They must be public, since the TestFn class will be instantiated under a separate ClassLoader;
    // therefore we need broader access than might be anticipated.
    public static volatile Result result = null;
    public static volatile Throwable exception = null;
    public static volatile Integer staticConfig = null;
    public static volatile Integer count = 0;

}
