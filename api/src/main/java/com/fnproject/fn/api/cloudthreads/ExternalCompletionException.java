package com.fnproject.fn.api.cloudthreads;

/**
 * Exception thrown when an externally-completable future is failed.
 */
public class ExternalCompletionException extends RuntimeException {
    private final HttpRequest externalRequest;

    public ExternalCompletionException(HttpRequest externalRequest) {
        super(new String(externalRequest.getBodyAsBytes()));
        this.externalRequest = externalRequest;
    }

    public HttpRequest getExternalRequest() {
        return externalRequest;
    }
}
