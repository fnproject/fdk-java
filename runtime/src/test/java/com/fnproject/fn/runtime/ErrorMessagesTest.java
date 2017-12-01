package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.testfns.ErrorMessages;
import not.in.com.fnproject.fn.StacktraceFilteringTestFunctions;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ErrorMessagesTest {

    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    private void assertIsErrorWithoutStacktrace(String errorMessage) {
        assertThat(fn.exitStatus()).isEqualTo(2);
        assertThat(fn.getStdErrAsString()).contains(errorMessage);
        assertThat(fn.getStdErrAsString().split(System.getProperty("line.separator")).length).isEqualTo(1);
    }

    private void assertIsEntrypointErrorWithStacktrace(String errorMessage) {
        assertThat(fn.exitStatus()).isEqualTo(2);
        assertThat(fn.getStdErrAsString()).contains(errorMessage);
        assertThat(fn.getStdErrAsString().split(System.getProperty("line.separator")).length).isGreaterThan(1);
        assertThat(fn.getStdErrAsString()).doesNotContain("at com.fnproject.fn.runtime");
    }

    private void assertIsFunctionErrorWithStacktrace(String errorMessage) {
        assertThat(fn.exitStatus()).isEqualTo(1);
        assertThat(fn.getStdErrAsString()).contains(errorMessage);
        assertThat(fn.getStdErrAsString().split(System.getProperty("line.separator")).length).isGreaterThan(1);
        assertThat(fn.getStdErrAsString()).doesNotContain("at com.fnproject.fn.runtime");
    }

    /* The test method names should be mentally prefixed with "we get a nice error message when..." */

    @Test
    public void userSpecifiesNonExistentClass(){
        fn.thenRun("NonExistentClass", "method");
        assertIsErrorWithoutStacktrace("Class 'NonExistentClass' not found in function jar. It's likely that the 'cmd' entry in func.yaml is incorrect.");
    }

    @Test
    public void userSpecifiesClassWithNoMethods(){
        fn.thenRun(ErrorMessages.NoMethodsClass.class, "thisClassHasNoMethods");
        assertIsErrorWithoutStacktrace("Method 'thisClassHasNoMethods' was not found in class 'com.fnproject.fn.runtime.testfns.ErrorMessages.NoMethodsClass'. Available functions were: []");
    }

    @Test
    public void userSpecifiesMethodWhichDoesNotExist(){
        fn.thenRun(ErrorMessages.OneMethodClass.class, "notTheMethod");
        assertIsErrorWithoutStacktrace("Method 'notTheMethod' was not found in class 'com.fnproject.fn.runtime.testfns.ErrorMessages.OneMethodClass'. Available functions were: [theMethod]");
    }

    @Test
    public void userFunctionInputCoercionError(){
        fn.givenDefaultEvent().withBody("This is not a...").enqueue();
        fn.thenRun(ErrorMessages.OtherMethodsClass.class, "takesAnInteger");
        assertIsEntrypointErrorWithStacktrace("An exception was thrown during Input Coercion: Failed to coerce event to user function parameter type class java.lang.Integer");
    }

    @Test
    public void objectConstructionThrowsARuntimeException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.ExceptionInConstructor.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Whoops");
    }

    @Test
    public void objectConstructionThrowsADeepException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.DeepExceptionInConstructor.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Inside a method called by the constructor");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$DeepExceptionInConstructor.naughtyMethod");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$DeepExceptionInConstructor.<init>");
    }

    @Test
    public void objectConstructionThrowsANestedException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.NestedExceptionInConstructor.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Caused by: java.lang.RuntimeException: Oh no!");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$NestedExceptionInConstructor.naughtyMethod");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.ArithmeticException: / by zero");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$NestedExceptionInConstructor.naughtyMethod");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$NestedExceptionInConstructor.<init>");
    }

    @Test
    public void fnConfigurationThrowsARuntimeException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.ExceptionInConfiguration.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Caused by: java.lang.RuntimeException: Config fail");
    }

    @Test
    public void fnConfigurationThrowsADeepException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.DeepExceptionInConfiguration.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Caused by: java.lang.RuntimeException: Deep config fail");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$DeepExceptionInConfiguration.throwDeep");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$DeepExceptionInConfiguration.config");
    }

    @Test
    public void fnConfigurationThrowsANestedException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.NestedExceptionInConfiguration.class, "invoke");
        assertIsEntrypointErrorWithStacktrace("Error invoking configuration method: config");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: nested at 3");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: nested at 2");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: nested at 1");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: Deep config fail");
        assertThat(fn.getStdErrAsString()).contains("at not.in.com.fnproject.fn.StacktraceFilteringTestFunctions$NestedExceptionInConfiguration.config");
    }

    @Test
    public void functionThrowsNestedException(){
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(StacktraceFilteringTestFunctions.CauseStackTraceInResult.class, "invoke");
        assertIsFunctionErrorWithStacktrace("An error occurred in function: Throw two");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: Throw two");
        assertThat(fn.getStdErrAsString()).contains("Caused by: java.lang.RuntimeException: Throw one");


    }

}
