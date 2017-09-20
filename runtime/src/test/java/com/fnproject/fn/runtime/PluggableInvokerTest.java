package com.fnproject.fn.runtime;

import com.fnproject.fn.api.*;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PluggableInvokerTest {

    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @Test
    public void shouldRunAlternativeEntryPoint() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "handleRequest");
        assertThat(fn.getParsedHttpResponses().size()).isEqualTo(1);
        assertThat(fn.getParsedHttpResponses().get(0).getBodyAsString()).isEqualTo("foo");
    }

    public static class TestFn {
        @FnConfiguration
        public void configure(RuntimeContext ctx) {
            ctx.setInvoker((ctx1, evt) -> Optional.of(invoke(evt)));
        }

        public static OutputEvent invoke(InputEvent evt) {
            return OutputEvent.fromBytes("foo".getBytes(), true, "text/plain");
        }

        public void handleRequest() {}
    }
}
