package com.fnproject.fn.runtime.httpgateway;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InvocationContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 20/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FunctionHTTPGatewayContextTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    @Mock
    InvocationContext ctx;

    @Test
    public void shouldCreateGatewayContextFromInputs() {
        Headers h = Headers.emptyHeaders()
          .setHeader("H1", "h1val")
          .setHeader("Fn-Http-Method", "PATCH")
          .setHeader("Fn-Http-Request-Url", "http://blah.com?a=b&c=d&c=e")
          .setHeader("Fn-Http-H-", "ignored")
          .setHeader("Fn-Http-H-A", "b")
          .setHeader("Fn-Http-H-mv", "c", "d");

        Mockito.when(ctx.getRequestHeaders()).thenReturn(h);

        FunctionHTTPGatewayContext hctx = new FunctionHTTPGatewayContext(ctx);

        assertThat(hctx.getHeaders())
          .isEqualTo(Headers.emptyHeaders()
            .addHeader("A", "b")
            .addHeader("mv", "c", "d"));


        assertThat(hctx.getRequestURL())
          .isEqualTo("http://blah.com?a=b&c=d&c=e");

        assertThat(hctx.getQueryParameters().get("a")).contains("b");
        assertThat(hctx.getQueryParameters().getValues("c")).contains("d", "e");

        assertThat(hctx.getMethod()).isEqualTo("PATCH");


    }


    @Test
    public void shouldCreateGatewayContextFromEmptyHeaders() {

        Mockito.when(ctx.getRequestHeaders()).thenReturn(Headers.emptyHeaders());

        FunctionHTTPGatewayContext hctx = new FunctionHTTPGatewayContext(ctx);

        assertThat(hctx.getMethod()).isEqualTo("");
        assertThat(hctx.getRequestURL()).isEqualTo("");
        assertThat(hctx.getHeaders()).isEqualTo(Headers.emptyHeaders());
        assertThat(hctx.getQueryParameters().getAll()).isEmpty();

    }


    @Test
    public void shouldPassThroughResponseAttributes() {

        Mockito.when(ctx.getRequestHeaders()).thenReturn(Headers.emptyHeaders());

        FunctionHTTPGatewayContext hctx = new FunctionHTTPGatewayContext(ctx);
        hctx.setResponseHeader("My-Header", "foo", "bar");
        Mockito.verify(ctx).setResponseHeader("Fn-Http-H-My-Header", "foo", "bar");

        hctx.setResponseHeader("Content-Type", "my/ct", "ignored");
        Mockito.verify(ctx).setResponseContentType("my/ct");
        Mockito.verify(ctx).setResponseHeader("Fn-Http-H-Content-Type", "my/ct");

        hctx.addResponseHeader("new-H", "v1");
        Mockito.verify(ctx).addResponseHeader("Fn-Http-H-new-H", "v1");

        hctx.setStatusCode(101);


        Mockito.verify(ctx).setResponseHeader("Fn-Http-Status", "101");

    }

}
