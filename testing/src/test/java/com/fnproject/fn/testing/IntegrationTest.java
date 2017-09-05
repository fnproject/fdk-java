package com.fnproject.fn.testing;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    @Test
    public void runIntegrationTests() {

        fn.givenFn("nonexistent/nonexistent")
                .withFunctionError()

                .givenFn("appName/route")
                .withAction((body) -> {
                    if (new String(body).equals("PASS")) {
                        return "okay".getBytes();
                    } else {
                        throw new FunctionError("failed as demanded");
                    }
                })
                .givenEvent()
                .withBody("")   // or "1,5,6,32" to select a set of tests individually
                .enqueue()

                .thenRun(ExerciseEverything.class, "handleRequest");

        assertThat(fn.getResults().get(0).getBodyAsString())
                .endsWith("Everything worked\n");
    }
}
