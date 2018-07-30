package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

import java.util.Map;

/**
 * Test function class for e2e tests
 */
public class TestFnWithConfigurationMethods {

    public static class StaticTargetNoConfiguration {
        public static String echo(String s) {
            return "StaticTargetNoConfiguration\n" + s;
        }
    }

    public static class InstanceTargetNoConfiguration {
        public String echo(String s) {
            return "InstanceTargetNoConfiguration\n" + s;
        }
    }

    public static class StaticTargetStaticConfiguration {
        static String toPrint = null;

        @FnConfiguration
        public static void config(RuntimeContext ctx) {
            toPrint = "StaticTargetStaticConfiguration\n";
        }

        public static String echo(String s) {
            return toPrint + s;
        }
    }

    public static class WithGetConfigurationByKey {
        private final String param;

        public WithGetConfigurationByKey(RuntimeContext ctx) {
            param = ctx.getConfigurationByKey("PARAM").orElse("default");
        }

        public String getParam() { return param; }
    }

    public static class InstanceTargetStaticConfiguration {
        static String toPrint = null;

        @FnConfiguration
        public static void config(RuntimeContext ctx) {
            toPrint = "InstanceTargetStaticConfiguration\n";
        }

        public static String echo(String s) {
            return toPrint + s;
        }
    }

    public static class StaticTargetInstanceConfiguration {
        String toPrint = null;

        @FnConfiguration
        public void config(RuntimeContext ctx) {
            toPrint = "StaticTargetInstanceConfiguration\n";
        }

        public static String echo(String s) throws Exception {
            // This is a static method so it cannot access the instance
            // variable that we set in configuration. This is actually a
            // configuration error case and so this code will not run.
            throw new Exception("This code should not be run!");
        }
    }

    public static class InstanceTargetInstanceConfiguration {
        String toPrint = null;

        @FnConfiguration
        public void config(RuntimeContext ctx) {
            toPrint = "InstanceTargetInstanceConfiguration\n";
        }

        public String echo(String s) {
            return toPrint + s;
        }
    }

    public static class StaticTargetStaticConfigurationNoRuntime {
        static String toPrint = null;

        @FnConfiguration
        public static void config() {
            toPrint = "StaticTargetStaticConfigurationNoRuntime\n";
        }

        public static String echo(String s) {
            return toPrint + s;
        }
    }

    public static class InstanceTargetStaticConfigurationNoRuntime {
        static String toPrint = null;

        @FnConfiguration
        public static void config() {
            toPrint = "InstanceTargetStaticConfigurationNoRuntime\n";
        }

        public static String echo(String s) {
            return toPrint + s;
        }
    }

    public static class InstanceTargetInstanceConfigurationNoRuntime {
        String toPrint = null;

        @FnConfiguration
        public void config() {
            toPrint = "InstanceTargetInstanceConfigurationNoRuntime\n";
        }

        public String echo(String s) {
            return toPrint + s;
        }
    }

    public static class ConfigurationMethodIsNonVoid {
        @FnConfiguration
        public static String config() {
            return "ConfigurationMethodIsNonVoid\n";
        }

        public static String echo(String s) throws Exception {
            // This code will not run as the runtime should error out because
            // the configuration method is non-void.
            throw new Exception("This code should not be run!");
        }
    }


    public static class ConfigurationMethodWithAccessToConfig {
        private static Map<String, String> configFromContext;

        @FnConfiguration
        public static void configure(RuntimeContext ctx) {
            configFromContext = ctx.getConfiguration();
        }

        public static String configByKey(String key) {
            return "ConfigurationMethodWithAccessToConfig\n" + configFromContext.get(key);
        }
    }


    public interface CallOrdered {
        void called(String point);
    }

    public interface ConfigBaseInterface extends CallOrdered {

        @FnConfiguration
        default void ignoredInterfaceDefaultMethod() {
            throw new RuntimeException("bad");
        }

        // Interface static methods are excluded
        @FnConfiguration
        static void ignoredStaticConfigMethod() {
            throw new RuntimeException("bad");


        }

        void overriddenInterface();

    }

    public static abstract class ConfigBaseClass implements ConfigBaseInterface ,CallOrdered{
        public static String order = "";

        @Override
        public void called(String point){
            order += point;
        }

        @FnConfiguration
        public static void baseStatic1() {
            order += ".baseStatic1";
        }


        @FnConfiguration
        public void baseFn1() {
            called(".baseFn1");
        }

        @FnConfiguration
        public void baseFn2() {
            called( ".baseFn2");
        }

        @FnConfiguration
        public abstract void orNotRun();

        public abstract void orRun();

        public void notConfig() {
            throw new RuntimeException("bad");
        }
    }

    public interface ConfigSubInterface extends CallOrdered{
        void overriddenSubInterface();

        @FnConfiguration
        default void ignoredConfigDefaultMethod() {
            throw new RuntimeException("bad");
        }

        @FnConfiguration
        static void ignoredSubStaticConfigMethod() {
            throw new RuntimeException("bad");
        }
    }

    public static class SubConfigClass extends ConfigBaseClass implements ConfigSubInterface {


        @FnConfiguration
        public static void subStatic1() {
            order += ".subStatic1";
        }


        @FnConfiguration
        public void subFn1() {
            called(".subFn1");
        }

        @FnConfiguration
        public void overriddenSubInterface() {
            called(".subFn2");
        }

        @FnConfiguration
        public void overriddenInterface() {
            called(".subFn3");
        }


        @Override
        public void orNotRun() { // overrides without annotations are not called
            throw new RuntimeException("bad");
        }

        @Override
        @FnConfiguration
        public void orRun() {
            order += ".subFn4";
        }


        public String invoke() {
            return "OK";
        }

    }

}
