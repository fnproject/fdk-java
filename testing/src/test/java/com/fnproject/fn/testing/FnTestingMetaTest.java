package com.fnproject.fn.testing;

import com.fnproject.fn.api.cloudthreads.CloudThreads;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FnTestingMetaTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public static class TestFn {
        public String handleRequest() {
            try {
                CloudThreads.currentRuntime()
                        .completedValue(5);
            } catch (IllegalStateException expectedException) {
                return "OK";
            }
            return "FAIL";
        }
    }

    @Test
    public void mustComplainEarlyAboutUseOfCompleterFunctionsWithNoWiredCompleter() {
        fn.givenEvent().enqueue();
        fn.thenRun(TestFn.class, "handleRequest");
        assertThat(fn.getOnlyResult().getBodyAsString()).isEqualTo("OK");
    }
}
