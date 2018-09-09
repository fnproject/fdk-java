package com.fnproject.fn.testing;

import java.io.IOException;
import java.io.InputStream;

/**
 * Builder for function input events
 */
public interface FnEventBuilder<T> {

    /**
     * Add a header to the input with a variable number of values; duplicate headers will be overwritten
     *
     * @param key   header key
     * @param value header value(s)
     * @return an event builder
     */
    FnEventBuilder<T> withHeader(String key, String value);

    /**
     * Set the body of the request by providing an InputStream
     * <p>
     * Note - setting the body to an input stream means that only one event can be enqueued using this builder.
     *
     * @param body          the bytes of the body
     * @return an event builder
     */
    FnEventBuilder<T> withBody(InputStream body) throws IOException;

    /**
     * Set the body of the request as a byte array
     *
     * @param body the bytes of the body
     * @return an event builder
     */
    FnEventBuilder<T> withBody(byte[] body);

    /**
     * Set the body of the request as a String
     *
     * @param body the String of the body
     * @return an event builder
     */
    FnEventBuilder<T> withBody(String body);

    /**
     * Consume the builder and enqueue this event to be passed into the function when it is run
     *
     * @return The original testing rule. The builder is consumed.
     * @throws IllegalStateException if this event has already been enqueued and the event input can only be read once.
     */
//    FnTestingRule enqueue();
    T enqueue();

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
//    FnTestingRule enqueue(int n);
    T enqueue(int n);
}
