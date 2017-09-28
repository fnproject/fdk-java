package com.fnproject.fn.experimental.reduce;

import com.fnproject.fn.testing.FnTestingRule;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ReduceTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static class TestFn {
        public Integer handleRequest() {
            Flow fl = Flows.currentFlow();
            return Reduce.allResults(
                fl.supply(() -> 1),
                fl.supply(() -> 2),
                fl.supply(() -> 3),
                fl.supply(() -> 4),
                fl.supply(() -> 5)
            ).thenApply(results ->
                results.stream().map(f -> f.get()).reduce(0, (a, x) -> a + x)
            ).get();
        }
    }

    @Test
    public void reduceIteratesOverResults() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "handleRequest");

        assertEquals("15", fn.getOnlyResult().getBodyAsString());
    }

}