package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.testfns.*;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for custom data binding
 */
public class DataBindingTest {

    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @Test
    public void shouldUseInputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithConfig.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseInputCoercionSpecifiedWithAnnotation() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithAnnotation.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseOutputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomOutputDataBindingFnWithConfig.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseOutputCoercionSpecifiedWithAnnotation() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomOutputDataBindingFnWithAnnotation.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseFirstInputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithMultipleCoercions.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("HELLO WORLD");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseFirstOutputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomOutputDataBindingFnWithMultipleCoercions.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("HELLO WORLD");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseBuiltInInputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithNoUserCoercions.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Hello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseBuiltInOutputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomOutputDataBindingFnWithNoUserCoercions.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Hello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseSecondInputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithDudCoercion.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldUseSecondOutputCoercionSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomOutputDataBindingFnWithDudCoercion.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("dlroW olleH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldApplyCoercionsForInputAndOutputSpecifiedOnFunctionRuntimeContext() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnInputOutput.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("DLROW OLLEH");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldPrioritiseAnnotationOverConfig() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(CustomDataBindingFnWithAnnotationAndConfig.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("HELLO WORLD");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

}
