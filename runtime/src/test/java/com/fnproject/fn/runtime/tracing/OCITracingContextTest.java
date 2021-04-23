package com.fnproject.fn.runtime.tracing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OCITracingContextTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    InvocationContext ctxMock;

    @Mock
    FunctionRuntimeContext runtimeContextMock;

    @Mock
    MethodWrapper methodWrapperMock;

    private Map<String, String> getConfig(Boolean enabled) {
        Map<String, String> env = new HashMap<>();
        env.put("FN_APP_NAME", "myapp");
        env.put("FN_FN_NAME", "myFunction");
        env.put("OCI_TRACE_COLLECTOR_URL", "tracingPath");
        env.put("OCI_TRACING_ENABLED", enabled ? "1" : "0");
        return Collections.unmodifiableMap(env);
    }

    @Test
    public void configureRuntimeContext() {
        Map<String, String> config = getConfig(false);
        runtimeContextMock = new FunctionRuntimeContext(methodWrapperMock, config);

        OCITracingContext tracingContext = new OCITracingContext(ctxMock, runtimeContextMock);
        assertThat(tracingContext.isTracingEnabled()).isEqualTo(false);
        assertThat(tracingContext.getAppName()).isEqualToIgnoringCase("myapp");
        assertThat(tracingContext.getFunctionName()).isEqualToIgnoringCase("myFunction");
        assertThat(tracingContext.getTraceCollectorURL()).isEqualToIgnoringCase("tracingPath");
    }

    @Test
    public void getServiceName() {
        Map<String, String> config = getConfig(false);
        runtimeContextMock = new FunctionRuntimeContext(methodWrapperMock, config);

        OCITracingContext tracingContext = new OCITracingContext(ctxMock, runtimeContextMock);
        assertThat(tracingContext.getServiceName()).isEqualToIgnoringCase("myapp::myFunction");
    }

    @Test
    public void shouldAbleToConfigureWithNoHeaderData() {
        Map<String, String> config = getConfig(true);
        runtimeContextMock = new FunctionRuntimeContext(methodWrapperMock, config);
        Mockito.when(ctxMock.getRequestHeaders()).thenReturn(Headers.emptyHeaders());

        OCITracingContext tracingContext = new OCITracingContext(ctxMock, runtimeContextMock);
        assertThat(tracingContext.isSampled()).isEqualTo(true);
        assertThat(tracingContext.getTraceId()).isEqualTo("1");
        assertThat(tracingContext.getSpanId()).isEqualTo("1");
        assertThat(tracingContext.getParentSpanId()).isEqualTo("1");
    }

    @Test
    public void shouldAbleToConfigureWithHeaderDataNotSampled() {
        Map<String, String> config = getConfig(true);
        runtimeContextMock = new FunctionRuntimeContext(methodWrapperMock, config);
        Map<String, String> headerData = new HashMap();
        headerData.put("x-b3-sampled","0");
        Headers headers = Headers.fromMap(headerData);
        Mockito.when(ctxMock.getRequestHeaders()).thenReturn(headers);

        OCITracingContext tracingContext = new OCITracingContext(ctxMock, runtimeContextMock);
        assertThat(tracingContext.isSampled()).isEqualTo(false);
    }

    @Test
    public void shouldAbleToConfigureWithHeaderData() {
        Map<String, String> config = getConfig(true);
        runtimeContextMock = new FunctionRuntimeContext(methodWrapperMock, config);
        Map<String, String> headerData = new HashMap();
        headerData.put("x-b3-sampled","1");
        headerData.put("x-b3-flags","<implementation-specific data>");
        headerData.put("x-b3-traceid","213454321432");
        headerData.put("x-b3-spanid","244342r343");
        headerData.put("x-b3-parentspanid","32142r231242");
        Headers headers = Headers.fromMap(headerData);
        Mockito.when(ctxMock.getRequestHeaders()).thenReturn(headers);

        OCITracingContext tracingContext = new OCITracingContext(ctxMock, runtimeContextMock);
        assertThat(tracingContext.isSampled()).isEqualTo(true);
        assertThat(tracingContext.getTraceId()).isEqualTo("213454321432");
        assertThat(tracingContext.getSpanId()).isEqualTo("244342r343");
        assertThat(tracingContext.getParentSpanId()).isEqualTo("32142r231242");
        assertThat(tracingContext.getFlags()).isEqualTo("<implementation-specific data>");
    }
}
