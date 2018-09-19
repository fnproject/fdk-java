package com.fnproject.fn.testing.flow;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.runtime.flow.FlowFeature;
import com.fnproject.fn.testing.FnTestingRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Semaphore;

public class MultipleEventsTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public FlowTesting flow =  FlowTesting.create(fn);

    public static Semaphore oneGo = null;
    public static Semaphore twoGo = null;
    public static boolean success = false;

    @FnFeature(FlowFeature.class)
    public static class TestFn {
        public void handleRequest(String s) {
            switch (s) {
                case "1":
                    Flows.currentFlow().supply(() -> one());
                    break;
                case "2":
                    try {
                        MultipleEventsTest.twoGo.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Flows.currentFlow().supply(() -> two());
                    break;
            }
        }

        static void one() {
            System.err.println("In one, making rt");
            Flow fl1 = Flows.currentFlow();
            System.err.println("In one, completedValue(1)");
            fl1.completedValue(1);

            System.err.println("One: Does fl1 == currentFlow? " + (fl1 == Flows.currentFlow()));

            System.err.println("In one, letting two proceed");
            MultipleEventsTest.twoGo.release();

            System.err.println("In one, awaiting go signal");
            try {
                MultipleEventsTest.oneGo.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.err.println("In one, making second rt");
            Flow fl3 = Flows.currentFlow();
            System.err.println("In one, completedValue(3)");
            fl3.completedValue(3);

            success = fl1 == fl3;
            System.err.println("One: Does fl3 == currentFlow? " + (fl3 == Flows.currentFlow()));
            System.err.println("One: Does fl1 == fl3? " + success);

            MultipleEventsTest.twoGo.release();
            System.err.println("one completes");
        }

        static void two() {
            System.err.println("In two, awaiting signal to proceed");

            System.err.println("In two, making rt");
            Flow fl2 = Flows.currentFlow();
            System.err.println("In two, completedValue(2)");
            fl2.completedValue(2);

            System.err.println("Two: Does fl2 == currentFlow? " + (fl2 == Flows.currentFlow()));

            System.err.println("In two, letting one proceed");
            MultipleEventsTest.oneGo.release();

            try {
                MultipleEventsTest.twoGo.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("two completes");
        }
    }

    @Test
    public void OverlappingFlowInvocationsShouldWork() {
        fn.addSharedClass(MultipleEventsTest.class);

        oneGo = new Semaphore(0);
        twoGo = new Semaphore(0);
        success = false;

        fn.givenEvent()
                .withBody("1")
                .enqueue()
                .givenEvent()
                .withBody("2")
                .enqueue();

        fn.thenRun(TestFn.class, "handleRequest");

        Assertions.assertThat(success).isTrue();
    }

}
