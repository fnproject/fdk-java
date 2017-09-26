package com.fnproject.fn.experimental.retry;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class BasicRetryTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static AtomicInteger cas = new AtomicInteger(0);
    public static AtomicReference<String> failStr = new AtomicReference<>("yellow");

    public static class testMethods {
        public void handleSuccess() {
            Flow f = Flows.currentFlow();
            RetryOpts opts = new RetryOpts(new MaxAttemptStopStrategy(5), new ExponentialDelayStrategy(200, TimeUnit.MILLISECONDS, 2000));
            f.completedValue(1)
                    .thenCompose((a) -> Retry.retryWithOpts(() -> f.completedValue(1).thenRun(() -> BasicRetryTest.cas.compareAndSet(0, 2)), opts));
        }

        public void handleFailure() {
            Flow f = Flows.currentFlow();
            RetryOpts opts = new RetryOpts(new MaxAttemptStopStrategy(10), new FibonacciDelayStrategy(10, TimeUnit.MILLISECONDS, 100));
            f.completedValue(1)
                    .thenCompose((a) -> Retry.retryWithOpts(() -> f.failedFuture(new Exception("aaah")), opts))
                    .exceptionally((e) -> {
                        BasicRetryTest.failStr.set(e.getMessage());
                        return 0;
                    });
        }
    }

    @Test
    public void retryDoesNotChangeBehaviour() {
        fn.addSharedClass(BasicRetryTest.class);

        cas.set(0);

        fn.givenEvent().enqueue();

        fn.thenRun(testMethods.class, "handleSuccess");

        assertEquals(2, cas.get());
    }

    @Test
    public void retryDoesNotLoop() {
        fn.addSharedClass(BasicRetryTest.class);

        fn.givenEvent().enqueue();

        fn.thenRun(testMethods.class, "handleFailure");

        assertEquals("aaah", failStr.get());
    }
}
