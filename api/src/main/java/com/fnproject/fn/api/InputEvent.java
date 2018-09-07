package com.fnproject.fn.api;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Date;
import java.util.function.Function;

public interface InputEvent extends Closeable {

    /**
     * Consume the body associated with this event
     * <p>
     * This may only be done once per request.
     *
     * @param dest a function to send the body to - this does not need to close the incoming stream
     * @param <T>  An optional return type
     * @return the new
     */
    <T> T consumeBody(Function<InputStream, T> dest);



    /**
     * return the current call ID for this event
     * @return a call ID
     */
    String getCallID();


    /**
     * return the deadline by which this event should be processed - this is information an is intended  to help you determine how long you should spend processing your event - if you exceed this deadline Fn will terminate your container ¬
     *
     * @return a deadline relative to the current system clock that the event must be processed by
     */
    Date getDeadline();


    /**
     * The HTTP headers on the request
     *
     * @return an immutable map of headers
     */
    Headers getHeaders();


}
