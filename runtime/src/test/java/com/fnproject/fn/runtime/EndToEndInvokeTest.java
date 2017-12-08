package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.testfns.BadTestFnDuplicateMethods;
import com.fnproject.fn.runtime.testfns.TestFn;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests on Stdin/Stdout contract
 */
public class EndToEndInvokeTest {

    @Rule
    public final FnTestHarness fn = new FnTestHarness();

    @BeforeClass
    public static void setup() {
        TestFn.reset();
    }


    @Test
    public void shouldResolveTestCallWithEnvVarParams() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();
        TestFn.setOutput("Hello World Out");

        fn.thenRun(TestFn.class, "fnStringInOut");

        assertThat(TestFn.getInput()).isEqualTo("Hello World");
        assertThat(fn.getStdOutAsString()).isEqualTo("Hello World Out");
        assertThat(fn.getStdErrAsString()).isEmpty();
        assertThat(fn.exitStatus()).isZero();
    }

    @Test
    public void shouldResolveTestCallFromHotCall() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();
        TestFn.setOutput("Hello World Out");

        fn.thenRun(TestFn.class, "fnStringInOut");

        assertThat(TestFn.getInput()).isEqualTo("Hello World");
    }

    @Test
    public void shouldSerializeGenericCollections() throws Exception {
        fn.givenDefaultEvent().withBody("four").enqueue();

        fn.thenRun(TestFn.class, "fnGenericCollections");

        assertThat(fn.getStdOutAsString()).isEqualTo("[\"one\",\"two\",\"three\",\"four\"]");
    }

    @Test
    public void shouldSerializeAnimalCollections() throws Exception {
        fn.givenDefaultEvent().enqueue();

        fn.thenRun(TestFn.class, "fnGenericAnimal");

        assertThat(fn.getStdOutAsString()).isEqualTo("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]");
    }

    @Test
    public void shouldDeserializeGenericCollections() throws Exception {
        fn.givenDefaultEvent()
                .withHeader("Content-type", "application/json")
                .withBody("[\"one\",\"two\",\"three\",\"four\"]")
                .enqueue();

        fn.thenRun(TestFn.class, "fnGenericCollectionsInput");

        assertThat(fn.getStdOutAsString()).isEqualTo("ONE");
    }

    @Test
    public void shouldDeserializeCustomObjects() throws Exception {
        fn.givenDefaultEvent()
                .withHeader("Content-type", "application/json")
                .withBody("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]")
                .enqueue();

        fn.thenRun(TestFn.class, "fnCustomObjectsCollectionsInput");

        assertThat(fn.getStdOutAsString()).isEqualTo("Spot");
    }

    @Test
    public void shouldDeserializeComplexCustomObjects() throws Exception {
        fn.givenDefaultEvent()
                .withHeader("Content-type", "application/json")
                .withBody("{\"number1\":[{\"name\":\"Spot\",\"age\":6}]," +
                        "\"number2\":[{\"name\":\"Spot\",\"age\":16}]}")
                .enqueue();

        fn.thenRun(TestFn.class, "fnCustomObjectsNestedCollectionsInput");

        assertThat(fn.getStdOutAsString()).isEqualTo("Spot");
    }

    @Test
    public void shouldHandledStreamedHotInputEvent() throws Exception {
        fn.givenEvent()
                .withBody("message1")
                .withMethod("POST")
                .enqueue();

        fn.givenEvent()
                .withBody("message2")
                .withMethod("GET")
                .enqueue();


        fn.thenRun(TestFn.class,"fnEcho");

        List<FnTestHarness.ParsedHttpResponse> responses = fn.getParsedHttpResponses();

        assertThat(responses).size().isEqualTo(2);

        FnTestHarness.ParsedHttpResponse r1 = responses.get(0);
        assertThat(r1.getBodyAsString()).isEqualTo("message1");

        FnTestHarness.ParsedHttpResponse r2 = responses.get(1);
        assertThat(r2.getBodyAsString()).isEqualTo("message2");




    }


    @Test
    public void shouldPrintErrorOnUnknownMethod() throws Exception {


        fn.thenRun(TestFn.class, "unknownMethod");
        assertThat(fn.getStdOutAsString()).isEqualTo("");
        assertThat(fn.getStdErrAsString()).startsWith("Method 'unknownMethod' was not found in class 'com.fnproject.fn.runtime.testfns.TestFn'");

    }


    @Test
    public void shouldPrintErrorOnUnknownClass() throws Exception {


        fn.thenRun("com.fnproject.unknown.Class", "unknownMethod");

        assertThat(fn.getStdOutAsString()).isEqualTo("");
        assertThat(fn.getStdErrAsString()).startsWith("Class 'com.fnproject.unknown.Class' not found in function jar");

    }


    @Test
    public void shouldDirectStdOutToStdErrForFunctions() throws Exception {
        fn.givenDefaultEvent().enqueue();


        fn.thenRun(TestFn.class, "fnWritesToStdout");

        assertThat(fn.getStdOutAsString()).isEqualTo("");
        assertThat(fn.getStdErrAsString()).isEqualTo("STDOUT");

    }

    @Test
    public void shouldTerminateDefaultContainerOnExceptionWithError() throws Exception {
        fn.givenDefaultEvent().enqueue();
        fn.thenRun(TestFn.class, "fnThrowsException");
        assertThat(fn.getStdErrAsString()).startsWith("An error occurred in function:");
        assertThat(fn.getStdOutAsString()).isEmpty();
        assertThat(fn.exitStatus()).isEqualTo(1);
    }


    @Test
    public void shouldReadJsonObject() throws Exception {
        fn.givenDefaultEvent()
                .withHeader("Content-type", "application/json")
                .withBody("{\"foo\":\"bar\"}")
                .enqueue();


        fn.thenRun(TestFn.class, "fnReadsJsonObj");

        assertThat(((TestFn.JsonData) TestFn.getInput()).foo).isEqualTo("bar");
    }

    @Test
    public void shouldWriteJsonData() throws Exception {
        fn.givenDefaultEvent().enqueue();
        TestFn.JsonData data = new TestFn.JsonData();
        data.foo = "bar";
        TestFn.setOutput(data);

        fn.thenRun(TestFn.class, "fnWritesJsonObj");

        assertThat(fn.getStdOutAsString()).isEqualTo("{\"foo\":\"bar\"}");


    }

    @Test
    public void shouldReadBytesOnDefaultCodec() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFn.class, "fnReadsBytes");

        assertThat(TestFn.getInput()).isEqualTo("Hello World".getBytes());

    }


    @Test
    public void shouldWriteBytesOnDefaultCodec() throws Exception {
        fn.givenDefaultEvent().withBody("Hello World").enqueue();
        TestFn.setOutput("OK".getBytes());

        fn.thenRun(TestFn.class, "fnWritesBytes");

        assertThat(fn.getStdOutAsString()).isEqualTo("OK");

    }


    @Test
    public void shouldRejectDuplicateMethodsInFunctionClass() throws Exception {

        fn.thenRun(BadTestFnDuplicateMethods.class,"fn");

        assertThat(fn.getStdOutAsString()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith("Multiple methods match");
    }

    @Test
    public void shouldReadRawJson() throws Exception {
        fn.givenDefaultEvent()
                .withHeader("Content-type", "application/json")
                .withBody("[\"foo\",\"bar\"]")
                .enqueue();


        fn.thenRun(TestFn.class, "fnReadsRawJson");

        assertThat(TestFn.getInput()).isEqualTo(Arrays.asList("foo", "bar"));

    }



    @Test
    public void shouldReadMultipleMessageWhenInputIsNotParsed() throws Exception {
        fn.givenHttpEvent().withBody("Hello World 1").enqueue();
        fn.givenHttpEvent().withBody("Hello World 2").enqueue();


        fn.thenRun(TestFn.class, "readSecondInput");

        List<FnTestHarness.ParsedHttpResponse> results = fn.getParsedHttpResponses();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStatus()).isEqualTo(200);
        assertThat(results.get(0).getBodyAsString()).isEqualTo("first;");
        assertThat(results.get(1).getStatus()).isEqualTo(200);
        assertThat(results.get(1).getBodyAsString()).isEqualTo("Hello World 2");


    }


}
