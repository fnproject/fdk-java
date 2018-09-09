package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.testfns.TestFnWithConfigurationMethods;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for function configuration methods
 */
public class ConfigurationMethodsTest {

    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @Test
    public void staticTargetWithNoConfigurationIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetNoConfiguration.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("StaticTargetNoConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithNoConfigurationIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetNoConfiguration.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("InstanceTargetNoConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithStaticConfigurationIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetStaticConfiguration.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("StaticTargetStaticConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithStaticConfigurationIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetStaticConfiguration.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("InstanceTargetStaticConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithInstanceConfigurationIsAnError() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        String expectedMessage = "Configuration method " +
                "'config'" +
                " cannot be an instance method if the function method is a static method";

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetInstanceConfiguration.class, "echo");

        assertThat(fn.getOutputs()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith(expectedMessage);
        assertThat(fn.exitStatus()).isEqualTo(2);
    }

    @Test
    public void instanceTargetWithInstanceConfigurationIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetInstanceConfiguration.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("InstanceTargetInstanceConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithStaticConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetStaticConfigurationNoRuntime.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("StaticTargetStaticConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithStaticConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetStaticConfigurationNoRuntime.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("InstanceTargetStaticConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithInstanceConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetInstanceConfigurationNoRuntime.class, "echo");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("InstanceTargetInstanceConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldReturnDefaultParameterIfNotProvided() {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.WithGetConfigurationByKey.class, "getParam");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("default");
    }

    @Test
    public  void shouldReturnSetConfigParameterWhenProvided() {
        String value = "value";
        fn.setConfig("PARAM", value);
        fn.givenEvent().enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.WithGetConfigurationByKey.class, "getParam");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo(value);
    }

    @Test
    public void nonVoidConfigurationMethodIsAnError() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodIsNonVoid.class, "echo");

        String expectedMessage = "Configuration method " +
                "'config'" +
                " does not have a void return type";

        assertThat(fn.getOutputs()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith(expectedMessage);
        assertThat(fn.exitStatus()).isEqualTo(2);
    }


    @Test
    public void shouldBeAbleToAccessConfigInConfigurationMethodWhenDefault() {
        fn.setConfig("FOO", "BAR");
        fn.givenEvent()
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldBeAbleToAccessConfigInConfigurationMethodWhenHttp() {
        fn.setConfig("FOO", "BAR");
        fn.givenEvent()
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getOnlyOutputAsString()).contains("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldOnlyExtractConfigFromEnvironmentNotHeaderWhenHttp() {
        fn.givenEvent()
                .withHeader("FOO", "BAR")
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getOnlyOutputAsString()).doesNotContain("BAR");
    }

    @Test
    public void shouldNotBeAbleToAccessHeadersInConfigurationWhenDefault() {
        fn.givenEvent()
                .withHeader("FOO", "BAR")
                .withBody("HEADER_FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getOnlyOutputAsString()).doesNotContain("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldNotBeAbleToAccessHeadersInConfigurationWhenHttp() {
        fn.givenEvent()
                .withHeader("FOO", "BAR")
                .withBody("HEADER_FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getOnlyOutputAsString()).doesNotContain("ConfigurationMethodWithAccessToConfig\nBAR");
    }


    @Test
    public void shouldCallInheritedConfigMethodsInRightOrder() {
        fn.givenEvent().enqueue();
        TestFnWithConfigurationMethods.SubConfigClass.order = "";

        fn.thenRun(TestFnWithConfigurationMethods.SubConfigClass.class, "invoke");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("OK");
        assertThat(TestFnWithConfigurationMethods.SubConfigClass.order)
                .matches("\\.baseStatic1\\.subStatic1\\.baseFn\\d\\.baseFn\\d\\.subFn\\d\\.subFn\\d\\.subFn\\d\\.subFn\\d");
    }


}
