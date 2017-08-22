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
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetNoConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("StaticTargetNoConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithNoConfigurationIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetNoConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("InstanceTargetNoConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithStaticConfigurationIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetStaticConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("StaticTargetStaticConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithStaticConfigurationIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetStaticConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("InstanceTargetStaticConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithInstanceConfigurationIsAnError() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        String expectedMessage = "Configuration method " +
                "'config'" +
                " cannot be an instance method if the function method is a static method";

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetInstanceConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith(expectedMessage);
        assertThat(fn.exitStatus()).isEqualTo(2);
    }

    @Test
    public void instanceTargetWithInstanceConfigurationIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetInstanceConfiguration.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("InstanceTargetInstanceConfiguration\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void staticTargetWithStaticConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.StaticTargetStaticConfigurationNoRuntime.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("StaticTargetStaticConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithStaticConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetStaticConfigurationNoRuntime.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("InstanceTargetStaticConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void instanceTargetWithInstanceConfigurationWithoutRuntimeContextParameterIsOK() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.InstanceTargetInstanceConfigurationNoRuntime.class, "echo");

        assertThat(fn.getStdOutAsString()).isEqualTo("InstanceTargetInstanceConfigurationNoRuntime\nHello World");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldReturnDefaultParameterIfNotProvided() {
        fn.givenDefaultEvent().enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.WithGetConfigurationByKey.class, "getParam");

        assertThat(fn.getStdOutAsString()).isEqualTo("default");
    }

    @Test
    public  void shouldReturnSetConfigParameterWhenProvided() {
        String value = "value";
        fn.setConfig("PARAM", value);
        fn.givenDefaultEvent().enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.WithGetConfigurationByKey.class, "getParam");

        assertThat(fn.getStdOutAsString()).isEqualTo(value);
    }

    @Test
    public void nonVoidConfigurationMethodIsAnError() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodIsNonVoid.class, "echo");

        String expectedMessage = "Configuration method " +
                "'config'" +
                " does not have a void return type";

        assertThat(fn.getStdOutAsString()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith(expectedMessage);
        assertThat(fn.exitStatus()).isEqualTo(2);
    }


    @Test
    public void shouldBeAbleToAccessConfigInConfigurationMethodWhenDefault() {
        fn.setConfig("FOO", "BAR");
        fn.givenDefaultEvent()
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getStdOutAsString()).isEqualTo("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldBeAbleToAccessConfigInConfigurationMethodWhenHttp() {
        fn.setConfig("FOO", "BAR");
        fn.givenEvent()
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getStdOutAsString()).contains("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldOnlyExtractConfigFromEnvironmentNotHeaderWhenHttp() {
        fn.givenEvent()
                .withHeader("FOO", "BAR")
                .withBody("FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getStdOutAsString()).doesNotContain("BAR");
    }

    @Test
    public void shouldNotBeAbleToAccessHeadersInConfigurationWhenDefault() {
        fn.givenDefaultEvent()
                .withHeader("FOO", "BAR")
                .withBody("HEADER_FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getStdOutAsString()).doesNotContain("ConfigurationMethodWithAccessToConfig\nBAR");
    }

    @Test
    public void shouldNotBeAbleToAccessHeadersInConfigurationWhenHttp() {
        fn.givenEvent()
                .withHeader("FOO", "BAR")
                .withBody("HEADER_FOO")
                .enqueue();

        fn.thenRun(TestFnWithConfigurationMethods.ConfigurationMethodWithAccessToConfig.class, "configByKey");

        assertThat(fn.getStdOutAsString()).doesNotContain("ConfigurationMethodWithAccessToConfig\nBAR");
    }


    @Test
    public void shouldCallInheritedConfigMethodsInRightOrder() {
        fn.givenDefaultEvent().enqueue();
        TestFnWithConfigurationMethods.SubConfigClass.order = "";

        fn.thenRun(TestFnWithConfigurationMethods.SubConfigClass.class, "invoke");

        assertThat(fn.getStdOutAsString()).isEqualTo("OK");
        assertThat(TestFnWithConfigurationMethods.SubConfigClass.order)
                .matches("\\.baseStatic1\\.subStatic1\\.baseFn\\d\\.baseFn\\d\\.subFn\\d\\.subFn\\d\\.subFn\\d\\.subFn\\d");
    }


}
