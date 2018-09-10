package com.fnproject.fn.testing.flow;

import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.fn.testing.FunctionError;
import com.fnproject.fn.testing.flowtestfns.ExerciseEverything;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class IntegrationTest {

    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    public FlowTesting flow = FlowTesting.create(fn);

    @Test
    public void runIntegrationTests() {

        flow.givenFn("testFunctionNonExistant")
          .withFunctionError()

          .givenFn("testFunction")
          .withAction((body) -> {
              if (new String(body).equals("PASS")) {
                  return "okay".getBytes();
              } else {
                  throw new FunctionError("failed as demanded");
              }
          });

        fn
          .givenEvent()
          .withBody("")   // or "1,5,6,32" to select a set of tests individually
          .enqueue()

          .thenRun(ExerciseEverything.class, "handleRequest");

        Assertions.assertThat(fn.getResults().get(0).getBodyAsString())
          .endsWith("Everything worked\n");
    }
}
