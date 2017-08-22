package com.fnproject.fn.api.cloudthreads;

import java.net.URI;

/**
 * A variant of {@link CloudFuture} that can be completed via an HTTP request.
 * <p>
 * This can be used to integrate one-off events with a cloud thread by passing the completion and/or failure URL to a third party system in order for it to trigger an event within the thread.
 * <p>
 * For successful external futures, POST should include the desired body of the external event:
 *
 * <pre>
 *  POST /path/to/completion/url HTTP/1.1
 *  Host: hostname
 *  Content-type: application/json
 *
 *  {"result" : "good"}
 * </pre>
 *
 * The content type may be anything and the future will yield the byte array of the posted value back to the thread.
 *
 *
 * For Failures the
 *
 */
public interface ExternalCloudFuture<V> extends CloudFuture<V> {

    /**
     * The URL to post data to to complete this future normally.
     * @return a URL that completes this future normally
     */
    URI completionUrl();

    /**
     * The URL to post data to complete this future exceptionally.
     *
     * @return a URL that completes this future Exceptionally
     */
    URI failUrl();
}
