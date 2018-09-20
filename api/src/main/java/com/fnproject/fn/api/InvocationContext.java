package com.fnproject.fn.api;


/**
 * Context wrapper around a single invocation.
 * <p>
 * Any attributes attached to this interface are scoped solely to a single function invocation;
 * multiple invocations of a hot function will receive new instances of this interface.
 */
public interface InvocationContext {

    /**
     * Returns the {@link RuntimeContext} associated with this invocation context
     *
     * @return a runtime context
     */
    RuntimeContext getRuntimeContext();

    /**
     * Adds an {@link InvocationListener} that will be fired when an invocation of this function
     * completes either successfully or exceptionally.
     *
     * @param listener a listener to fire when function completes execution
     */
    void addListener(InvocationListener listener);


    /**
     * Returns the current request headers for the invocation
     *
     * @return the headers passed into the function
     */
    Headers getRequestHeaders();

    /**
     * Sets the response content type,  this will override the default content type of the output
     *
     * @param contentType a mime type for the response
     */
    default void setResponseContentType(String contentType) {
        this.setResponseHeader(OutputEvent.CONTENT_TYPE_HEADER, contentType);
    }

    /**
     * Adds a response header to the outbound event
     *
     * @param key   header key
     * @param value header value
     */
    void addResponseHeader(String key, String value);

    /**
     * Sets a response header to the outbound event, overriding a previous value.
     * <p>
     * Headers set in this way override any headers returned by the function or any middleware on the function
     *
     * @param key header key
     * @param v1  first value to set
     * @param vs  other values to set header to
     */
    void setResponseHeader(String key, String v1, String... vs);
}
