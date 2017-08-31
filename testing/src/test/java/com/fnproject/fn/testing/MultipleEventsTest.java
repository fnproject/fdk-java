package com.fnproject.fn.testing;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleEventsTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static Semaphore oneGo = null;
    public static Semaphore twoGo = null;
    public static boolean success = false;

    public static class TestFn {
        public void handleRequest(String s) {
            switch (s) {
                case "1":
                    CloudThreads.currentRuntime().supply(() -> one());
                    break;
                case "2":
                    try {
                        MultipleEventsTest.twoGo.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    CloudThreads.currentRuntime().supply(() -> two());
                    break;
            }
        }

        static void one() {
            System.err.println("In one, making rt");
            CloudThreadRuntime rt1 = CloudThreads.currentRuntime();
            System.err.println("In one, completedValue(1)");
            rt1.completedValue(1);

            System.err.println("One: Does rt1 == currentRuntime? " + (rt1 == CloudThreads.currentRuntime()));

            System.err.println("In one, letting two proceed");
            MultipleEventsTest.twoGo.release();

            System.err.println("In one, awaiting go signal");
            try {
                MultipleEventsTest.oneGo.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.err.println("In one, making second rt");
            CloudThreadRuntime rt3 = CloudThreads.currentRuntime();
            System.err.println("In one, completedValue(3)");
            rt3.completedValue(3);

            success = rt1 == rt3;
            System.err.println("One: Does rt3 == currentRuntime? " + (rt3 == CloudThreads.currentRuntime()));
            System.err.println("One: Does rt1 == rt3? " + success);

            MultipleEventsTest.twoGo.release();
            System.err.println("one completes");
        }

        static void two() {
            System.err.println("In two, awaiting signal to proceed");

            System.err.println("In two, making rt");
            CloudThreadRuntime rt2 = CloudThreads.currentRuntime();
            System.err.println("In two, completedValue(2)");
            rt2.completedValue(2);

            System.err.println("Two: Does rt2 == currentRuntime? " + (rt2 == CloudThreads.currentRuntime()));

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
    public void OverlappingThreadInvocationsShouldWork() {
        fn.addMirroredClass(TestFn.class.getName());

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

        assertThat(success).isTrue();
    }

}
