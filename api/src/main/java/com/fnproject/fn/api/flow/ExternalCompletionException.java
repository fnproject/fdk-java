package com.fnproject.fn.api.flow;

/**
 * Exception thrown into the graph when an an external future is marked as failed
 *
 * This captures the original HttpRequest details (including headers and body) of the API call to the external completion endpoint.
 */
public class ExternalCompletionException extends RuntimeException {
    private final HttpRequest externalRequest;

    public ExternalCompletionException(HttpRequest externalRequest) {
        super("External completion failed");
        this.externalRequest = externalRequest;
    }

    /**
     * The original request that triggered this failure
     *
     * @return details of the HTTP request that was use to trigger a failure.
     */
    public HttpRequest getExternalRequest() {
        return externalRequest;
    }
}
