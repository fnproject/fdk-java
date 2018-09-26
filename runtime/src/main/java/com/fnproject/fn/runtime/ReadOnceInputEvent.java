package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Wrapper for an incoming fn invocation event
 * <p>
 * This in
 */
public class ReadOnceInputEvent implements InputEvent {
    private final BufferedInputStream body;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final Headers headers;
    private final Instant deadline;
    private final String callID;


    public ReadOnceInputEvent(InputStream body, Headers headers, String callID, Instant deadline) {
        this.body = new BufferedInputStream(Objects.requireNonNull(body, "body"));
        this.headers = Objects.requireNonNull(headers, "headers");
        this.callID = Objects.requireNonNull(callID, "callID");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        body.mark(Integer.MAX_VALUE);
    }


    /**
     * Consume the input stream of this event  -
     * This may be done exactly once per event
     *
     * @param dest a consumer for the body
     * @throws IllegalStateException if the input has been consumed
     */
    @Override
    public <T> T consumeBody(Function<InputStream, T> dest) {
        if (consumed.compareAndSet(false, true)) {
            try (InputStream rb = body) {
                return dest.apply(rb);
            } catch (IOException e) {
                throw new FunctionInputHandlingException("Error reading input stream", e);
            }
        } else {
            throw new IllegalStateException("Body has already been consumed");
        }

    }


    @Override
    public String getCallID() {
        return callID;
    }

    @Override
    public Instant getDeadline() {
        return deadline;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }


    @Override
    public void close() throws IOException {
        body.close();
    }
}
