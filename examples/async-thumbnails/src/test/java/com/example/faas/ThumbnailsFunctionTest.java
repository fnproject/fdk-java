package com.example.faas;

import com.fnproject.fn.examples.ThumbnailsFunction;
import com.fnproject.fn.testing.FnTestingRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ThumbnailsFunctionTest {


    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Rule
    public final WireMockRule mockServer = new WireMockRule(0);

    @Test
    public void testThumbnail() {
        testing
                .addMirroredClass(ThumbnailsFunction.class)

                .setConfig("OBJECT_STORAGE_URL", "http://localhost:" + mockServer.port())
                .setConfig("OBJECT_STORAGE_ACCESS", "alpha")
                .setConfig("OBJECT_STORAGE_SECRET", "betabetabetabeta")


                .givenFn("myapp/resize128")
                    .withAction((data) -> "128".getBytes())
                .givenFn("myapp/resize256")
                    .withAction((data) -> "256".getBytes())
                .givenFn("myapp/resize512")
                    .withAction((data) -> "512".getBytes())

                .givenEvent()
                    .withBody("testing".getBytes())
                    .enqueue();

        // Mock the http endpoint
        mockMinio();

        testing.thenRun(ThumbnailsFunction.class, "handleRequest");

        // Check the final image uploads were performed
        mockServer.verify(1, putRequestedFor(urlMatching("/alpha/.*\\.png")).withRequestBody(equalTo("testing")));
        mockServer.verify(1, putRequestedFor(urlMatching("/alpha/.*\\.png")).withRequestBody(equalTo("128")));
        mockServer.verify(1, putRequestedFor(urlMatching("/alpha/.*\\.png")).withRequestBody(equalTo("256")));
        mockServer.verify(1, putRequestedFor(urlMatching("/alpha/.*\\.png")).withRequestBody(equalTo("512")));
        mockServer.verify(4, putRequestedFor(urlMatching(".*")));
    }

    @Test
    public void anExternalFunctionFailure() {
        testing
                .addMirroredClass(ThumbnailsFunction.class)

                .setConfig("OBJECT_STORAGE_URL", "http://localhost:" + mockServer.port())
                .setConfig("OBJECT_STORAGE_ACCESS", "alpha")
                .setConfig("OBJECT_STORAGE_SECRET", "betabetabetabeta")

                .givenFn("myapp/resize128")
                    .withResult("128".getBytes())
                .givenFn("myapp/resize256")
                    .withResult("256".getBytes())
                .givenFn("myapp/resize512")
                    .withFunctionError()

                .givenEvent()
                    .withBody("testing".getBytes())
                    .enqueue();

        // Mock the http endpoint
        mockMinio();

        testing.thenRun(ThumbnailsFunction.class, "handleRequest");

        // Confirm that one image upload didn't happen
        mockServer.verify(0, putRequestedFor(urlMatching("/alpha/.*\\.png")).withRequestBody(equalTo("512")));

        mockServer.verify(3, putRequestedFor(urlMatching(".*")));

    }

    private void mockMinio() {

        mockServer.stubFor(get(urlMatching("/alpha.*"))
                .willReturn(aResponse().withBody(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ListBucketResult>\n" +
                        "    <Name>alpha</Name>\n" +
                        "    <Prefix/>\n" +
                        "    <KeyCount>0</KeyCount>\n" +
                        "    <MaxKeys>100</MaxKeys>\n" +
                        "    <IsTruncated>false</IsTruncated>\n" +
                        "</ListBucketResult>")));

        mockServer.stubFor(WireMock.head(urlMatching("/alpha.*")).willReturn(aResponse().withStatus(200)));

        mockServer.stubFor(WireMock.put(urlMatching(".*")).willReturn(aResponse().withStatus(200)));

    }

}
