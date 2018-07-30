package junit.com.fnproject.fn.testing;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FnTestingRuleTest {

    public static Map<String, String> configuration;
    public static InputEvent inEvent;
    public static List<InputEvent> capturedInputs = new ArrayList<>();
    public static List<byte[]> capturedBodies = new ArrayList<>();

    @Rule
    public        FnTestingRule fn             = FnTestingRule.createDefault();
    private final String        exampleBaseUrl = "http://www.example.com";

    @Before
    public void reset() {
        fn.addSharedClass(FnTestingRuleTest.class);
        fn.addSharedClass(InputEvent.class);


        FnTestingRuleTest.configuration = null;
        FnTestingRuleTest.inEvent = null;
        FnTestingRuleTest.capturedInputs = new ArrayList<>();
        FnTestingRuleTest.capturedBodies = new ArrayList<>();
    }


    public static class TestFn {
        private RuntimeContext ctx;

        public TestFn(RuntimeContext ctx) {
            this.ctx = ctx;
        }

        public void copyConfiguration() {
            configuration = new HashMap<>(ctx.getConfiguration());
        }

        public void copyInputEvent(InputEvent inEvent) {
            FnTestingRuleTest.inEvent = inEvent;
        }

        public void err() {
            throw new RuntimeException("ERR");
        }

        public void captureInput(InputEvent in) {
            capturedInputs.add(in);
            capturedBodies.add(in.consumeBody(TestFn::consumeToBytes));
        }

        private static byte[] consumeToBytes(InputStream is) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is, bos);
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        public OutputEvent echoInput(InputEvent in) {
            byte[] result = in.consumeBody(TestFn::consumeToBytes);
            return OutputEvent.fromBytes(result, OutputEvent.SUCCESS, "application/octet-stream");
        }

    }


    @Test
    public void shouldSetEnvironmentInsideFnScope() {
        fn.givenEvent().enqueue();
        fn.setConfig("CONFIG_FOO", "BAR");

        fn.thenRun(FnTestingRuleTest.TestFn.class, "copyConfiguration");

        Assertions.assertThat(configuration).containsEntry("CONFIG_FOO", "BAR");
    }


    @Test
    public void shouldCleanEnvironmentOfSpecialVarsInsideFnScope() {
        fn.givenEvent().enqueue();
        fn.setConfig("CONFIG_FOO", "BAR");

        fn.thenRun(FnTestingRuleTest.TestFn.class, "copyConfiguration");

        Assertions.assertThat(configuration).doesNotContainKeys("APP_NAME", "ROUTE", "METHOD", "REQUEST_URL");
    }


    @Test
    public void shouldHandleErrors() {
        fn.givenEvent().enqueue();

        fn.thenRun(FnTestingRuleTest.TestFn.class, "err");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(500);
        Assertions.assertThat(fn.getStdErrAsString()).contains("An error occurred in function: ERR");
    }


    @Test
    public void configShouldNotOverrideIntrinsicHeaders() {
        fn.givenEvent().enqueue();
        fn.setConfig("APP_NAME", "BAR");

        fn.thenRun(FnTestingRuleTest.TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getAppName()).isEqualTo("appName");
    }


    @Test
    public void configShouldBeCaptitalisedAndReplacedWithUnderscores() {// Basic test
        // Test uppercasing and mangling of keys
        fn.givenEvent().enqueue();

        fn.setConfig("some-key-with-dashes", "some-value");

        fn.thenRun(FnTestingRuleTest.TestFn.class, "copyConfiguration");

        Assertions.assertThat(configuration).containsEntry("SOME_KEY_WITH_DASHES", "some-value");

    }

    @Test
    public void shouldSetArgsInFirstEvent() {
        fn.givenEvent().withAppName("TEST_APP")
                .withHeader("H1", "H2")
                .withMethod("PUT")
                .withRequestUrl("http://example.com/mytest")
                .withRoute("/myroute")
                .enqueue();

        fn.thenRun(FnTestingRuleTest.TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getAppName()).isEqualTo("TEST_APP");
        Assertions.assertThat(inEvent.getRoute()).isEqualTo("/myroute");
        Assertions.assertThat(inEvent.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo("http://example.com/mytest");
    }


    @Test
    public void shouldSendEventDataToSDKInputEvent() {
        final String APP_NAME = "alpha";
        final String ROUTE = "/bravo";
        final String REQUEST_URL = "http://charlie/alpha/bravo";
        final String METHOD = "POST";

        fn.setConfig("SOME_CONFIG", "SOME_VALUE");
        fn.givenEvent()
                .withAppName(APP_NAME)
                .withRoute(ROUTE)
                .withRequestUrl(REQUEST_URL)
                .withMethod(METHOD)
                .withHeader("FOO", "BAR, BAZ")
                .withHeader("FEH", "")
                .withBody("Body") // body as string
                .enqueue();

        fn.thenRun(TestFn.class, "captureInput");

        FnResult result = fn.getOnlyResult();
        Assertions.assertThat(result.getBodyAsString()).isEmpty();
        Assertions.assertThat(result.getHeaders().getAll()).contains(headerEntry("Content-length", "0"));
        Assertions.assertThat(result.getStatus()).isEqualTo(200);

        InputEvent event = capturedInputs.get(0);
        Assertions.assertThat(event.getAppName()).isEqualTo(APP_NAME);
        Assertions.assertThat(event.getHeaders().getAll())
                  .contains(headerEntry("FOO", "BAR, BAZ"))
                  .contains(headerEntry("FEH", ""));
        Assertions.assertThat(event.getMethod()).isEqualTo(METHOD);
        Assertions.assertThat(capturedBodies.get(0)).isEqualTo("Body".getBytes());
    }


    @Test
    public void shouldEnqueueMultipleDistinctEvents() {
        fn.setConfig("SOME_CONFIG", "SOME_VALUE");
        fn.givenEvent()
                .withAppName("alpha")
                .withRoute("/bravo")
                .withRequestUrl("http://charlie/alpha/bravo")
                .withMethod("POST")
                .withHeader("FOO", "BAR")
                .withBody("Body") // body as string
                .enqueue();


        fn.givenEvent()
                .withAppName("alpha")
                .withRoute("/bravo2")
                .withRequestUrl("http://charlie/alpha/bravo2")
                .withMethod("PUT")
                .withHeader("FOO2", "BAR2")
                .withBody("Body2") // body as string
                .enqueue();

        fn.thenRun(TestFn.class, "captureInput");

        FnResult result = fn.getResults().get(0);
        Assertions.assertThat(result.getBodyAsString()).isEmpty();
        Assertions.assertThat(result.getHeaders().getAll()).contains(headerEntry("Content-length", "0"));
        Assertions.assertThat(result.getStatus()).isEqualTo(200);

        InputEvent event = capturedInputs.get(0);
        Assertions.assertThat(event.getAppName()).isEqualTo("alpha");
        Assertions.assertThat(event.getHeaders().getAll()).contains(headerEntry("FOO", "BAR"));
        Assertions.assertThat(event.getMethod()).isEqualTo("POST");
        Assertions.assertThat(capturedBodies.get(0)).isEqualTo("Body".getBytes());


        FnResult result2 = fn.getResults().get(1);
        Assertions.assertThat(result2.getBodyAsString()).isEmpty();
        Assertions.assertThat(result2.getHeaders().getAll()).contains(headerEntry("Content-length", "0"));
        Assertions.assertThat(result2.getStatus()).isEqualTo(200);

        InputEvent event2 = capturedInputs.get(1);
        Assertions.assertThat(event2.getAppName()).isEqualTo("alpha");
        Assertions.assertThat(event2.getHeaders().getAll()).contains(headerEntry("FOO2", "BAR2"));
        Assertions.assertThat(event2.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(capturedBodies.get(1)).isEqualTo("Body2".getBytes());
    }


    @Test
    public void shouldEnqueueMultipleIdenticalEvents() {
        fn.givenEvent()
                .withAppName("alpha")
                .withRoute("/bravo")
                .withRequestUrl("http://charlie/alpha/bravo")
                .withMethod("POST")
                .withHeader("FOO", "BAR")
                .withBody("Body") // body as string
                .enqueue(10);

        fn.thenRun(TestFn.class, "echoInput");

        List<FnResult> results = fn.getResults();
        Assertions.assertThat(results).hasSize(10);


        results.forEach((r) -> {
            Assertions.assertThat(r.getStatus()).isEqualTo(200);
            Assertions.assertThat(r.getHeaders().getAll())
                      .contains(headerEntry("Content-Type", "application/octet-stream"))
                      .contains(headerEntry("Content-length", String.valueOf("Body".getBytes().length)));

        });
    }


    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowSecondEnqueueOnInputStreamInput() {
        fn.givenEvent()
                .withAppName("alpha")
                .withRoute("/bravo")
                .withRequestUrl("http://charlie/alpha/bravo")
                .withMethod("POST")
                .withHeader("FOO", "BAR")
                .withBody(new ByteArrayInputStream("Body".getBytes()), 4) // body as string
                .enqueue(2);


    }

    @Test
    public void shouldEnqueuIndependentEventsWithInputStreams() {
        fn.givenEvent()
                .withBody(new ByteArrayInputStream("Body".getBytes()), 4) // body as string
                .enqueue();

        fn.givenEvent()
                .withBody(new ByteArrayInputStream("Body1".getBytes()), 5) // body as string
                .enqueue();

        fn.thenRun(TestFn.class, "echoInput");

        List<FnResult> results = fn.getResults();
        Assertions.assertThat(results).hasSize(2);

        Assertions.assertThat(results.get(0).getBodyAsString()).isEqualTo("Body");
        Assertions.assertThat(results.get(1).getBodyAsString()).isEqualTo("Body1");
    }

    @Test
    public void shouldHandleBodyAsInputStream() {
        fn.givenEvent().withBody(new ByteArrayInputStream("FOO BAR".getBytes()), 3).enqueue();

        fn.thenRun(TestFn.class, "captureInput");

        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(200);
        Assertions.assertThat(capturedBodies.get(0)).isEqualTo("FOO".getBytes());
    }

    @Test
    public void shouldLeaveQueryParamtersOffIfNotSpecified() {
        String baseUrl = "www.example.com";
        fn.givenEvent()
                .withRequestUrl(baseUrl)
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(baseUrl);
    }

    @Test
    public void shouldPrependQuestionMarkForFirstQueryParam() {
        String baseUrl = "www.example.com";
        fn.givenEvent()
                .withRequestUrl(baseUrl)
                .withQueryParameter("var", "val")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");
        Assertions.assertThat(fn.getOnlyResult().getStatus()).isEqualTo(200);
        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(baseUrl + "?var=val");
    }

    @Test
    public void shouldHandleMultipleQueryParameters() {
        String baseUrl = "www.example.com";
        fn.givenEvent()
                .withRequestUrl(baseUrl)
                .withQueryParameter("var1", "val1")
                .withQueryParameter("var2", "val2")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(baseUrl + "?var1=val1&var2=val2");
    }

    @Test
    public void shouldHandleMultipleQueryParametersWithSameKey() {
        String baseUrl = "www.example.com";
        fn.givenEvent()
                .withRequestUrl(baseUrl)
                .withQueryParameter("var", "val1")
                .withQueryParameter("var", "val2")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(baseUrl + "?var=val1&var=val2");
    }

    @Test
    public void shouldUrlEncodeQueryParameterKey() {
        fn.givenEvent()
                .withRequestUrl(exampleBaseUrl)
                .withQueryParameter("&", "val")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(exampleBaseUrl + "?%26=val");
    }

    @Test
    public void shouldHandleQueryParametersWithSpaces() {
        fn.givenEvent()
                .withRequestUrl(exampleBaseUrl)
                .withQueryParameter("my var", "this val")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(exampleBaseUrl + "?my+var=this+val");
    }

    @Test
    public void shouldUrlEncodeQueryParameterValue() {
        String baseUrl = "www.example.com";
        fn.givenEvent()
                .withRequestUrl(baseUrl)
                .withQueryParameter("var", "&")
                .enqueue();
        fn.thenRun(TestFn.class, "copyInputEvent");

        Assertions.assertThat(inEvent.getRequestUrl()).isEqualTo(baseUrl + "?var=%26");
    }

    private static Map.Entry<String, String> headerEntry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
