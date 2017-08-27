package com.fnproject.fn.testing;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Properties;

/**
 * Created on 27/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class FakeSystem {
    public final static InputStream in = null;
    public final static PrintStream out = null;
    public final static PrintStream err = null;


    public static void setIn(InputStream in) {

    }


    public static void setOut(PrintStream out) {

    }


    public static void setErr(PrintStream err) {

    }


    public static Console console() {
        return null;
    }


    public static Channel inheritedChannel() throws IOException {
        return SelectorProvider.provider().inheritedChannel();
    }

    public static void setSecurityManager(final SecurityManager s) {

    }


    public static SecurityManager getSecurityManager() {
        return null;
    }

    public static long currentTimeMillis() {
        return 0l;
    }


    public static long nanoTime() {
        return 0l;
    }

    public static void arraycopy(Object src, int srcPos,
                                 Object dest, int destPos,
                                 int length) {

    }


    public static int identityHashCode(Object x) {
        return 0;
    }


    public static Properties getProperties() {
        return null;
    }


    public static String lineSeparator() {
        return lineSeparator;
    }

    private static String lineSeparator;

    public static void setProperties(Properties props) {

    }

    public static String getProperty(String key) {
        return "";
    }


    public static String getProperty(String key, String def) {
        return "";
    }

    public static String setProperty(String key, String value) {
        return "";
    }


    public static String clearProperty(String key) {
        return "";
    }

    public static String getenv(String name) {
        return "";
    }


    public static java.util.Map<String, String> getenv() {
        return null;
    }


    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }


    public static void gc() {
        Runtime.getRuntime().gc();
    }


    public static void runFinalization() {
        Runtime.getRuntime().runFinalization();
    }


    public static void runFinalizersOnExit(boolean value) {
        Runtime.runFinalizersOnExit(value);
    }


    public static void load(String filename) {
    }


    public static void loadLibrary(String libname) {
    }

    public static String mapLibraryName(String libname) {
        return null;
    }

}
