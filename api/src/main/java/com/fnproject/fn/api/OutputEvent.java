package com.fnproject.fn.api;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper for an outgoing fn event
 */
public interface OutputEvent {

    String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * The outcome status of this function event
     * This determines how the platform will reflect this error to the customer and how it will treat the container after an error
     */
    enum Status {
        /**
         * The event was successfully processed
         */
        Success(200),
        /**
         * The Function code raised unhandled exception
         */
        FunctionError(502),
        /**
         * The Function code did not respond within a given timeout
         */
        FunctionTimeout(504),
        /**
         * An internal error occurred in the FDK
         */
        InternalError(500);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

    }


    /**
     * Report the outcome status code of this event.
     *
     * @return the status associated with this event
     */
    Status getStatus();


    /**
     * Report the boolean success of this event.
     * For default-format functions, this is used to map the HTTP status code into a straight success/failure.
     *
     * @return true if the output event results from a successful invocation.
     */
    default boolean isSuccess() {
        return getStatus() == Status.Success;
    }

    /**
     * The  content type of the response.
     * <p>
     *
     * @return The name of the content type.
     */
    default Optional<String> getContentType(){
        return getHeaders().get(CONTENT_TYPE_HEADER);
    }

    /**
     * Any additional {@link Headers} that should be supplied along with the content
     * <p>
     * These are only used when the function format is HTTP
     *
     * @return the headers to add
     */
    Headers getHeaders();

    /**
     * Write the body of the output to a stream
     *
     * @param out an outputstream to emit the body of the event
     * @throws IOException OutputStream exceptions percolate up through this method
     */
    void writeToOutput(OutputStream out) throws IOException;


    /**
     * Creates a new output event based on this one with the headers overriding
     * @param headers the headers use in place of this event
     * @return a new output event with these set
     */
    default OutputEvent withHeaders(Headers headers) {
        Objects.requireNonNull(headers, "headers");

        OutputEvent a = this;
        return new OutputEvent() {

            @Override
            public Status getStatus() {
                return a.getStatus();
            }

            @Override
            public Headers getHeaders() {
                return headers;
            }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {
                a.writeToOutput(out);
            }
        };
    }

    /**
     * Create an output event from a byte array
     *
     * @param bytes       the byte array to write to the output
     * @param status      the status code to report
     * @param contentType the content type to present on HTTP responses
     * @return a new output event
     */
    static OutputEvent fromBytes(byte[] bytes, Status status, String contentType) {
        return fromBytes(bytes, status, contentType, Headers.emptyHeaders());
    }

    /**
     * Create an output event from a byte array
     *
     * @param bytes       the byte array to write to the output
     * @param status      the status code of this event
     * @param contentType the content type to present on HTTP responses or null
     * @param headers     any additional headers to supply with HTTP responses
     * @return a new output event
     */
    static OutputEvent fromBytes(final byte[] bytes, final Status status, final String contentType, final Headers headers) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(headers, "headers");

        final Headers newHeaders = contentType== null?Headers.emptyHeaders():headers.setHeader("Content-Type",contentType);
        return new OutputEvent() {

            @Override
            public Status getStatus() {
                return status;
            }


            @Override
            public Headers getHeaders() {
                return newHeaders;
            }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {
                out.write(bytes);
            }
        };
    }

    /**
     * Returns an output event with an empty body and a given status
     * @param status the status of the event
     * @return a new output event
     */
    static OutputEvent emptyResult(final Status status) {
        Objects.requireNonNull(status, "status");

        return new OutputEvent() {
            @Override
            public Status getStatus() {
                return status;
            }

            @Override
            public Headers getHeaders() {
                return Headers.emptyHeaders();
            }

            @Override
            public void writeToOutput(OutputStream out) throws IOException {

            }
        };
    }
}
