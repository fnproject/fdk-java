package com.fnproject.fn.experimental.retry;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class BasicRetryTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static AtomicInteger cas = new AtomicInteger(0);

    public static class testMethods {
        public void handleSuccess() {
            Flow f = Flows.currentFlow();
            f.completedValue(1)
                    .thenCompose((a) -> Retry.retryExponentialWithJitter(() -> f.completedValue(1).thenRun(() -> BasicRetryTest.cas.compareAndSet(0, 2))));
        }

        public void handleFailure() throws Exception {
            Flow f = Flows.currentFlow();
            Exception e = (Exception) f.completedValue(1)
                    .thenCompose((a) -> Retry.retryExponentialWithJitter(() -> {throw new Exception("aaah");}))
                    .exceptionally((ex) -> ex).get();
            throw e;
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

        try {
            fn.thenRun(testMethods.class, "handleFailure");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "aaah");
            return;
        }
        assert false;
    }
}
