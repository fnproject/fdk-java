package com.fnproject.fn.runtime;

import com.fnproject.fn.api.InputEvent;
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
        fn.givenEvent().withBody("Hello World").enqueue();
        TestFn.setOutput("Hello World Out");

        fn.thenRun(TestFn.class, "fnStringInOut");

        assertThat(TestFn.getInput()).isEqualTo("Hello World");
        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Hello World Out");
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
        fn.givenEvent().withBody("four").enqueue();

        fn.thenRun(TestFn.class, "fnGenericCollections");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("[\"one\",\"two\",\"three\",\"four\"]");
    }

    @Test
    public void shouldSerializeAnimalCollections() throws Exception {
        fn.givenEvent().enqueue();

        fn.thenRun(TestFn.class, "fnGenericAnimal");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]");
    }

    @Test
    public void shouldDeserializeGenericCollections() throws Exception {
        fn.givenEvent()
          .withHeader("Content-type", "application/json")
          .withBody("[\"one\",\"two\",\"three\",\"four\"]")
          .enqueue();

        fn.thenRun(TestFn.class, "fnGenericCollectionsInput");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("ONE");
    }

    @Test
    public void shouldDeserializeCustomObjects() throws Exception {
        fn.givenEvent()
          .withHeader("Content-type", "application/json")
          .withBody("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]")
          .enqueue();

        fn.thenRun(TestFn.class, "fnCustomObjectsCollectionsInput");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Spot");
    }

    @Test
    public void shouldDeserializeComplexCustomObjects() throws Exception {
        fn.givenEvent()
          .withHeader("Content-type", "application/json")
          .withBody("{\"number1\":[{\"name\":\"Spot\",\"age\":6}]," +
            "\"number2\":[{\"name\":\"Spot\",\"age\":16}]}")
          .enqueue();

        fn.thenRun(TestFn.class, "fnCustomObjectsNestedCollectionsInput");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Spot");
    }

    @Test
    public void shouldHandledStreamedHotInputEvent() throws Exception {
        fn.givenEvent()
          .withBody("message1")
          .enqueue();

        fn.givenEvent()
          .withBody("message2")
          .enqueue();


        fn.thenRun(TestFn.class, "fnEcho");

        List<FnTestHarness.TestOutput> responses = fn.getOutputs();

        assertThat(responses).size().isEqualTo(2);

        FnTestHarness.TestOutput r1 = responses.get(0);
        assertThat(new String(r1.getBody())).isEqualTo("message1");

        FnTestHarness.TestOutput r2 = responses.get(1);
        assertThat(new String(r2.getBody())).isEqualTo("message2");


    }


    @Test
    public void shouldPrintErrorOnUnknownMethod() throws Exception {
        fn.thenRun(TestFn.class, "unknownMethod");
        assertThat(fn.exitStatus()).isEqualTo(2);
        assertThat(fn.getOutputs()).isEmpty();
        assertThat(fn.getStdErrAsString()).startsWith("Method 'unknownMethod' was not found in class 'com.fnproject.fn.runtime.testfns.TestFn'");
    }


    @Test
    public void shouldPrintErrorOnUnknownClass() throws Exception {


        fn.thenRun("com.fnproject.unknown.Class", "unknownMethod");
        assertThat(fn.exitStatus()).isEqualTo(2);
        assertThat(fn.getOutputs()).hasSize(0);
        assertThat(fn.getStdErrAsString()).startsWith("Class 'com.fnproject.unknown.Class' not found in function jar");

    }


    @Test
    public void shouldDirectStdOutToStdErrForFunctions() throws Exception {
        fn.givenEvent().enqueue();


        fn.thenRun(TestFn.class, "fnWritesToStdout");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("");
        assertThat(fn.getStdErrAsString()).isEqualTo("STDOUT");

    }

    @Test
    public void shouldTerminateDefaultContainerOnExceptionWithError() throws Exception {
        fn.givenEvent().enqueue();
        fn.thenRun(TestFn.class, "fnThrowsException");
        assertThat(fn.getStdErrAsString()).startsWith("An error occurred in function:");
        assertThat(fn.getOnlyOutputAsString()).isEmpty();
        assertThat(fn.exitStatus()).isEqualTo(1);
    }


    @Test
    public void shouldReadJsonObject() throws Exception {
        fn.givenEvent()
          .withHeader("Content-type", "application/json")
          .withBody("{\"foo\":\"bar\"}")
          .enqueue();


        fn.thenRun(TestFn.class, "fnReadsJsonObj");

        assertThat(((TestFn.JsonData) TestFn.getInput()).foo).isEqualTo("bar");
    }

    @Test
    public void shouldWriteJsonData() throws Exception {
        fn.givenEvent().enqueue();
        TestFn.JsonData data = new TestFn.JsonData();
        data.foo = "bar";
        TestFn.setOutput(data);

        fn.thenRun(TestFn.class, "fnWritesJsonObj");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("{\"foo\":\"bar\"}");


    }

    @Test
    public void shouldReadBytesOnDefaultCodec() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();

        fn.thenRun(TestFn.class, "fnReadsBytes");

        assertThat(TestFn.getInput()).isEqualTo("Hello World".getBytes());

    }

    @Test
    public void shouldPrintLogFrame() throws Exception {
        fn.setConfig("FN_LOGFRAME_NAME", "containerID");
        fn.setConfig("FN_LOGFRAME_HDR", "fnID");
        fn.givenEvent().withHeader("fnID", "fnIDVal").withBody( "Hello world!").enqueue();

        fn.thenRun(TestFn.class, "fnEcho");
        assertThat(fn.getOnlyOutputAsString()).isEqualTo("Hello world!");
        // stdout gets redirected to stderr - hence printing out twice
        assertThat(fn.getStdErrAsString()).isEqualTo("\ncontainerID=fnIDVal\n\n\ncontainerID=fnIDVal\n\n");

    }


    @Test
    public void shouldWriteBytesOnDefaultCodec() throws Exception {
        fn.givenEvent().withBody("Hello World").enqueue();
        TestFn.setOutput("OK".getBytes());

        fn.thenRun(TestFn.class, "fnWritesBytes");

        assertThat(fn.getOnlyOutputAsString()).isEqualTo("OK");

    }


    @Test
    public void shouldRejectDuplicateMethodsInFunctionClass() throws Exception {

        fn.thenRun(BadTestFnDuplicateMethods.class, "fn");
        assertThat(fn.getOutputs()).isEmpty();
        assertThat(fn.exitStatus()).isEqualTo(2);
        assertThat(fn.getStdErrAsString()).startsWith("Multiple methods match");
    }

    @Test
    public void shouldReadRawJson() throws Exception {
        fn.givenEvent()
          .withHeader("Content-type", "application/json")
          .withBody("[\"foo\",\"bar\"]")
          .enqueue();


        fn.thenRun(TestFn.class, "fnReadsRawJson");

        assertThat(TestFn.getInput()).isEqualTo(Arrays.asList("foo", "bar"));

    }


    @Test
    public void shouldReadInputHeaders() throws Exception{
        fn.givenEvent()
          .withHeader("myHeader", "Foo")
          .withHeader("a-n-header", "b0o","b10")
          .enqueue();
        fn.thenRun(TestFn.class, "readRawEvent");

        InputEvent iev = (InputEvent)TestFn.getInput();
        assertThat(iev).isNotNull();
        assertThat(iev.getHeaders().getAllValues("Myheader")).contains("Foo");
        assertThat(iev.getHeaders().getAllValues("A-N-Header")).contains("b0o","b10");


    }

    @Test
    public void shouldExposeOutputHeaders() throws Exception{
        fn.givenEvent()
          .enqueue();
        fn.thenRun(TestFn.class, "setsOutputHeaders");


        FnTestHarness.TestOutput to = fn.getOutputs().get(0);

        System.err.println("got response" + to );
        assertThat(to.getContentType()).contains("foo-ct");
        assertThat(to.getHeaders().get("Header-1")).contains("v1");
        assertThat(to.getHeaders().getAllValues("A")).contains("b1","b2");

    }
}
