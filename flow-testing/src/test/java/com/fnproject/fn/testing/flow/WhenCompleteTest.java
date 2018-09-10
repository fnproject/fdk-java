package com.fnproject.fn.testing.flow;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;
import com.fnproject.fn.testing.FnTestingRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class WhenCompleteTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();
    private FlowTesting flow = FlowTesting.create(fn);

    public static AtomicInteger cas = new AtomicInteger(0);


    @FnFeature(FlowFeature.class)
    public static class TestFn {
        public void handleRequest() {
            Flows.currentFlow().completedValue(1)
                    .whenComplete((v, e) -> WhenCompleteTest.cas.compareAndSet(0, 1))
                    .thenRun(() -> WhenCompleteTest.cas.compareAndSet(1, 2));
        }
    }

    @Test
    public void OverlappingFlowInvocationsShouldWork() {
        fn.addSharedClass(WhenCompleteTest.class);

        cas.set(0);

        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "handleRequest");

        Assertions.assertThat(cas.get()).isEqualTo(2);
    }

}
