package com.fnproject.fn.testing;

import java.io.InputStream;

/**
 * Builder for function input events
 */
public interface FnEventBuilder {

    /**
     * Add a header to the input with a variable number of values; duplicate headers will be overwritten
     *
     * @param key   header key
     * @param value header value(s)
     * @return an event builder
     */
    FnEventBuilder withHeader(String key, String value);

    /**
     * Set the body of the request by providing an InputStream
     * <p>
     * Note - setting the body to an input stream means that only one event can be enqueued using this builder.
     *
     * @param body          the bytes of the body
     * @param contentLength how long the body is supposed to be
     * @return an event builder
     */
    FnEventBuilder withBody(InputStream body, int contentLength);

    /**
     * Set the body of the request as a byte array
     *
     * @param body the bytes of the body
     * @return an event builder
     */
    FnEventBuilder withBody(byte[] body);

    /**
     * Set the body of the request as a String
     *
     * @param body the String of the body
     * @return an event builder
     */
    FnEventBuilder withBody(String body);

    /**
     * Set the app name associated with the call
     *
     * @param appName the app name
     * @return an event builder
     */
    FnEventBuilder withAppName(String appName);

    /**
     * Set the fn route associated with the call
     *
     * @param route the route
     * @return an event builder
     */
    FnEventBuilder withRoute(String route);

    /**
     * Set the HTTP method of the incoming request
     *
     * @param method an HTTP method
     * @return an event builder
     */
    FnEventBuilder withMethod(String method);

    /**
     * Set the request URL of the incoming event
     *
     * @param requestUrl the request URL
     * @return an event builder
     */
    FnEventBuilder withRequestUrl(String requestUrl);

    /**
     * Add a query parameter to the request URL
     *
     * @param key   - non URL encoded key
     * @param value - non URL encoded value
     * @return an event builder
     */
    FnEventBuilder withQueryParameter(String key, String value);

    /**
     * Consume the builder and enqueue this event to be passed into the function when it is run
     *
     * @return The original testing rule. The builder is consumed.
     * @throws IllegalStateException if this event has already been enqueued and the event input can only be read once.
     */
    FnTestingRule enqueue();

    /**
     * Consume the builder and enqueue multiple copies of this event.
     * <p>
     * Note that if the body of the event has been set to an input stream this will fail with an
     * {@link IllegalStateException}.
     *
     * @param n number of copies of the event to enqueue
     * @return The original testing rule. The builder is consumed.
     * @throws IllegalStateException if the body cannot be read multiple times.
     */
    FnTestingRule enqueue(int n);
}
