package com.fnproject.fn.api;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Wrapper for an outgoing fn event
 */
public interface OutputEvent {
    /**
     * Report the HTTP status code of this event.
     * For default-format functions, this value is mapped into a success/failure value as follows:
     * status codes in the range [100, 400) are considered successful; anything else is a failure.
     *
     * @return the status code associated with this event
     */
    int getStatusCode();

    int SUCCESS = 200;
    int FAILURE = 500;

    default boolean isSuccess() {
        return 100 <= getStatusCode() && getStatusCode() < 400;
    }

    /**
     * The indicative content type of the response.
     * <p>
     * This will only be used when the function format is HTTP
     *
     * @return The name of the content type.
     */
    Optional<String> getContentType();

    /**
     * Any additional {@link Headers} that should be supplied along with the content
     *
     * These are only used when the function format is HTTP
     *
     * @return the headers to add
     */
    Headers getHeaders();

    /**
     * Write the body of the output to a stream
     *
     * @param out           an outputstream to emit the body of the event
     * @throws IOException  OutputStream exceptions percolate up through this method
     */
    void writeToOutput(OutputStream out) throws IOException;


    /**
     * Create an output event from a byte array
     *
     * @param bytes       the byte array to write to the output
     * @param statusCode  the status code to report
     * @param contentType the content type to present on HTTP responses
     * @return a new output event
     */
     static OutputEvent fromBytes(byte[] bytes, int statusCode, String contentType) {
         return fromBytes(bytes, statusCode, contentType, Headers.emptyHeaders());
     }

    /**
     * Create an output event from a byte array
     *
     * @param bytes       the byte array to write to the output
     * @param statusCode  the HTTP status code of this event
     * @param contentType the content type to present on HTTP responses
     * @param headers     any additional headers to supply with HTTP responses
     * @return a new output event
     */
    static OutputEvent fromBytes(byte[] bytes, int statusCode, String contentType, Headers headers) {
        if (statusCode < 100 || 600 <= statusCode) {
            throw new IllegalArgumentException("Valid status codes must lie in the range [100, 599]");
        }
        return new OutputEvent() {

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public Optional<String> getContentType() {
                return Optional.ofNullable(contentType);
            }

            @Override
            public Headers getHeaders() { return headers; }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {
                out.write(bytes);
            }
        };
    }

    static OutputEvent emptyResult(int statusCode) {
        if (statusCode < 100 || 600 <= statusCode) {
            throw new IllegalArgumentException("Valid status codes must lie in the range [100, 599]");
        }
        return new OutputEvent() {
            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public Optional<String> getContentType() {
                return Optional.empty();
            }

            @Override
            public Headers getHeaders() { return Headers.emptyHeaders(); }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {

            }
        };
    }
}
