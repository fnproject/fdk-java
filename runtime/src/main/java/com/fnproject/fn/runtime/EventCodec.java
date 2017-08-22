package com.fnproject.fn.runtime;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;

import java.io.IOException;
import java.util.Optional;

/**
 * Event Codec - deals with different calling conventions between fn and the function docker container
 */
public interface EventCodec {
    /**
     * Read a event from the input
     *
     * @return an empty input stream if the end of the stream is reached or an event if otherwise
     */
    Optional<InputEvent> readEvent();

    /**
     * Should the codec be used again
     *
     * @return true if {@link #readEvent()} can read another message
     */
    boolean shouldContinue();

    /**
     * Write an event to the output
     *
     * @param evt event to write
     * @throws IOException if an error occurs while writing
     */
    void writeEvent(OutputEvent evt);

}
