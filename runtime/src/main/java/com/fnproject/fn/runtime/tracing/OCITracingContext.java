package com.fnproject.fn.runtime.tracing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.tracing.TracingContext;

public class OCITracingContext implements TracingContext {
    private final InvocationContext invocationContext;
    private final RuntimeContext runtimeContext;
    private String traceCollectorURL;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private Boolean sampled = true;
    private String flags;
    private Boolean tracingEnabled;
    private String appName;
    private String fnName;
    private static final String PLACEHOLDER_TRACE_COLLECTOR_URL = "http://localhost:9411/api/v2/span";
    private static final String PLACEHOLDER_TRACE_ID = "1";


    public OCITracingContext(InvocationContext invocationContext, RuntimeContext runtimeContext) {
        this.invocationContext = invocationContext;
        this.runtimeContext = runtimeContext;

        configureDefaultValue();
        configure(runtimeContext);

        if(tracingEnabled)
            configure(invocationContext.getRequestHeaders());
    }

    private void configureDefaultValue() {
        this.traceCollectorURL = PLACEHOLDER_TRACE_COLLECTOR_URL;
        this.traceId = PLACEHOLDER_TRACE_ID;
        this.spanId = PLACEHOLDER_TRACE_ID;
        this.parentSpanId = PLACEHOLDER_TRACE_ID;
    }

    private void configure(RuntimeContext runtimeContext) {
        if(runtimeContext != null && runtimeContext.getConfigurationByKey("OCI_TRACE_COLLECTOR_URL").get() != null
                && runtimeContext.getConfigurationByKey("OCI_TRACING_ENABLED").get() != null) {
            this.traceCollectorURL =
                    runtimeContext.getConfigurationByKey("OCI_TRACE_COLLECTOR_URL").get().isEmpty()
                            ?PLACEHOLDER_TRACE_COLLECTOR_URL
                            :runtimeContext.getConfigurationByKey("OCI_TRACE_COLLECTOR_URL").get();
            try {
                Integer tracingEnabledAsInt = Integer.parseInt(runtimeContext.getConfigurationByKey("OCI_TRACING_ENABLED").get());
                this.tracingEnabled = tracingEnabledAsInt != 0;
            } catch(java.lang.NumberFormatException ex) {
                this.tracingEnabled = false;
            }
            this.appName = runtimeContext.getAppName();
            this.fnName = runtimeContext.getFunctionName();
        }
    }

    private void configure(Headers headers) {
        this.flags = headers.get("x-b3-flags").orElse("");
        if (headers.get("x-b3-sampled").isPresent() && Integer.parseInt(headers.get("x-b3-sampled").get()) == 0) {
            this.sampled = false;
            return;
        }
        this.sampled = true;
        this.traceId = headers.get("x-b3-traceid").orElse(PLACEHOLDER_TRACE_ID);
        this.spanId = headers.get("x-b3-spanid").orElse(PLACEHOLDER_TRACE_ID);
        this.parentSpanId = headers.get("x-b3-parentspanid").orElse(PLACEHOLDER_TRACE_ID);
    }

    @Override
    public InvocationContext getInvocationContext() {
        return invocationContext;
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public String getServiceName() {
        return this.appName.toLowerCase() + "::" + this.fnName.toLowerCase();
    }

    @Override
    public String getTraceCollectorURL() {
        return traceCollectorURL;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public String getSpanId() {
        return spanId;
    }

    @Override
    public String getParentSpanId() {
        return parentSpanId;
    }

    @Override
    public Boolean isSampled() {
        return sampled;
    }

    @Override
    public String getFlags() {
        return flags;
    }

    @Override
    public Boolean isTracingEnabled() {
        return tracingEnabled;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getFunctionName() {
        return fnName;
    }
}
