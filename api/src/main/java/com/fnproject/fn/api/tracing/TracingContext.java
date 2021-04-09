package com.fnproject.fn.api.tracing;

import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.RuntimeContext;

public interface TracingContext {

    /**
     * Returns the underlying invocation context behind this Tracing context
     *
     * @return an invocation context related to this function
     */
    InvocationContext getInvocationContext();

    /**
     * Returns the {@link RuntimeContext} associated with this invocation context
     *
     * @return a runtime context
     */
    RuntimeContext getRuntimeContext();

    /**
     * Returns true if tracing is enabled for this function invocation
     *
     * @return whether tracing is enabled
     */
    Boolean isTracingEnabled();

    /**
     * Returns the user-friendly name of the application associated with the
     * function; shorthand for getRuntimeContext().getAppName()
     *
     * @return the user-friendly name of the application associated with the
     * function
     */
    String getAppName();

    /**
     * Returns the user-friendly name of the function; shorthand for
     * getRuntimeContext().getFunctionName()
     *
     * @return the user-friendly name of the function
     */
    String getFunctionName();

    /**
     * Returns a standard constructed "service name" to be used in tracing
     * libraries to identify the function
     *
     * @return a standard constructed "service name"
     */
    String getServiceName();

    /**
     * Returns the URL to be used in tracing libraries as the destination for
     * the tracing data
     *
     * @return a string containing the trace collector URL
     */
    String getTraceCollectorURL();

    /**
     * Returns the current trace ID as extracted from Zipkin B3 headers if they
     * are present on the request
     *
     * @return the trace ID as a string
     */
    String getTraceId();

    /**
     * Returns the current span ID as extracted from Zipkin B3 headers if they
     * are present on the request
     *
     * @return the span ID as a string
     */
    String getSpanId();

    /**
     * Returns the parent span ID as extracted from Zipkin B3 headers if they
     * are present on the request
     *
     * @return the parent span ID as a string
     */
    String getParentSpanId();

    /**
     * Returns the value of the Sampled header of the Zipkin B3 headers if they
     * are present on the request
     *
     * @return true if sampling is enabled for the request
     */
    Boolean isSampled();

    /**
     * Returns the value of the Flags header of the Zipkin B3 headers if they
     * are present on the request
     *
     * @return the verbatim value of the X-B3-Flags header
     */
    String getFlags();
}
