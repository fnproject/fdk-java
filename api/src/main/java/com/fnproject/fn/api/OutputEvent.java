package com.fnproject.fn.api;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Wrapper for an outgoing fn event
 */
public interface OutputEvent {
    /**
     * Is this invocation successful
     *
     * @return true if the invocation was successful or not
     */
    boolean isSuccess();

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
     * @param success     if the output is successful
     * @param contentType the content type to present on HTTP responses
     * @return a new output event
     */
     static OutputEvent fromBytes(byte[] bytes, boolean success, String contentType) {
         return fromBytes(bytes, success, contentType, Headers.emptyHeaders());
     }

    /**
     * Create an output event from a byte array
     *
     * @param bytes       the byte array to write to the output
     * @param success     if the output is successful
     * @param contentType the content type to present on HTTP responses
     * @param headers     any additional headers to supply with HTTP responses
     * @return a new output event
     */
    static OutputEvent fromBytes(byte[] bytes, boolean success, String contentType, Headers headers) {
        return new OutputEvent() {

            @Override
            public boolean isSuccess() {
                return success;
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

    static OutputEvent emptyResult(boolean success) {
        return new OutputEvent() {
            @Override
            public boolean isSuccess() {
                return success;
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
