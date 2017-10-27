package com.fnproject.fn.api;

import java.io.Closeable;
import java.io.InputStream;
import java.util.function.Function;

public interface InputEvent extends Closeable {

    /**
     * Consume the body associated with this event
     * <p>
     * This may only be done once per request, subsequent calls will throw a {@link IllegalStateException}
     *
     * @param dest a function to send the body to - this does not need to close the incoming stream
     * @param <T>  An optional return type
     * @return the new
     * @throws IllegalStateException when body has already been consumed
     */
    <T> T consumeBody(Function<InputStream, T> dest);

    /**
     * The application name associated with this function
     *
     * @return an application name
     */
    String getAppName();

    /**
     * @return The route (including preceding slash) of this function call
     * @deprecated use #getPath instead
     */
    @Deprecated()
    default String getRoute(){
        return getPath();
    }

    /**
     * @return The path (including preceding slash) of this function call
=     */
    String getPath();


    /**
     * @return The full request URL of this function invocation
     */
    String getRequestUrl();

    /**
     * The HTTP method used to invoke this function
     *
     * @return an UpperCase HTTP method
     */
    String getMethod();


    /**
     * The Fn callID for this event
     *
     * @return a stringified call ID
     */
    String getCallId();

    /**
     * The HTTP headers on the request
     *
     * @return an immutable map of headers
     */
    Headers getHeaders();

    /**
     * The query parameters of the function invocation
     *
     * @return an immutable map of query parameters parsed from the request URL
     */
    QueryParameters getQueryParameters();

}
