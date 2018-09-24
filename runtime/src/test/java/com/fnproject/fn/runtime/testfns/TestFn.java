package com.fnproject.fn.runtime.testfns;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test function class for e2e tests
 */
public class TestFn {
    private static final Object NOTHING = new Object();
    private static Object input = NOTHING;
    private static Object output = NOTHING;

    private static int count = 0;

    public static class JsonData {
        public String foo;
    }


    public String fnStringInOutInstance(String in) {
        input = in;
        return (String) output;
    }

    public static String fnStringInOut(String in) {
        input = in;
        return (String) output;
    }


    public static String fnEcho(String in) {
        return in;
    }

    public static void fnReadsBytes(byte[] in) {
        input = in;
    }


    public static byte[] fnWritesBytes() {
        return (byte[]) output;
    }


    public static void fnWritesToStdout() {
        System.out.print("STDOUT");
    }

    public static void fnThrowsException() {
        throw new RuntimeException("ERRTAG");
    }


    public static void fnReadsRawJson(List<String> strings) {
        input = strings;
    }

    public JsonData fnWritesJsonObj() {
        return (JsonData) output;

    }

    public static void fnReadsJsonObj(JsonData data) {
        input = data;
    }

    public static List<String> fnGenericCollections(String four) {
        return Arrays.asList("one", "two", "three", four);
    }

    public static String fnGenericCollectionsInput(List<String> strings) {
        return strings.get(0).toUpperCase();
    }

    public static String fnCustomObjectsCollectionsInput(List<Animal> animals) {
        return animals.get(0).getName();
    }

    public static String fnCustomObjectsNestedCollectionsInput(Map<String, List<Animal>> animals) {
        return animals.values().stream().findFirst().get().get(0).getName(); // !
    }


    public static String readSecondInput(InputEvent evt) {
        count++;
        if (count > 1) {
            return evt.consumeBody((is) -> {
                try {
                    return IOUtils.toString(is, StandardCharsets.UTF_8);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            return "first;";
        }

    }


    public static void readRawEvent(InputEvent evt) {
        input = evt;

    }


    public static void setsOutputHeaders(InvocationContext ic) {
        ic.addResponseHeader("Header-1","v1");
        ic.setResponseContentType("foo-ct");
        ic.addResponseHeader("a","b1");
        ic.addResponseHeader("a","b2");


    }
    public static List<Animal> fnGenericAnimal() {
        Animal dog = new Animal("Spot", 6);
        Animal cat = new Animal("Jason", 16);
        return Arrays.asList(dog, cat);
    }

    /**
     * Reset the internal (static) state
     * Should be called between runs;
     */
    public static  void reset() {
        input = NOTHING;
        output = NOTHING;
        count = 0;
    }


    public static Object getInput() {
        return input;
    }

    public static void setOutput(Object output) {
        TestFn.output = output;
    }
}
