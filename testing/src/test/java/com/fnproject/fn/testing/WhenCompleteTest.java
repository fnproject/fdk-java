package com.fnproject.fn.testing;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCompleteTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static AtomicInteger cas = new AtomicInteger(0);
    public static Semaphore go = null;

    public static class TestFn {
        public void handleRequest() {
            // This isn't great, but we're trying to ensure that the thenRun can be executed before whenComplete,
            // if possible.
            Flows.currentFlow().delay(1, TimeUnit.SECONDS).thenRun(() -> go.release());
            Flows.currentFlow().completedValue(1)
                    .whenComplete((v, e) -> {
                        try {
                            go.acquire();
                        } catch (InterruptedException e1) {}
                        WhenCompleteTest.cas.compareAndSet(0, 1);
                    })
                    .thenRun(() -> {
                        WhenCompleteTest.cas.compareAndSet(0, 2);
                        go.release();
                    });
        }
    }

    @Test
    public void OverlappingFlowInvocationsShouldWork() {
        fn.addSharedClass(WhenCompleteTest.class);

        cas.set(0);
        go = new Semaphore(0);

        fn.givenEvent()
                .enqueue();

        fn.thenRun(TestFn.class, "handleRequest");

        assertThat(cas.get()).isEqualTo(1);
    }

}
