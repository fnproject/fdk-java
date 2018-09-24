package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * DefaultEventCodec handles plain docker invocations on functions
 * <p>
 * This parses inputs from environment variables and reads and writes raw body and responses to the specified input and output streams
 *
 * @deprecated all new functions should use {@link HTTPStreamCodec}
 */
class DefaultEventCodec implements EventCodec {

    private final Map<String, String> env;
    private final InputStream in;
    private final OutputStream out;

    DefaultEventCodec(Map<String, String> env, InputStream in, OutputStream out) {
        this.env = env;
        this.in = in;
        this.out = out;
    }


    private String getRequiredEnv(String name) {
        String val = env.get(name);
        if (val == null) {
            throw new FunctionInputHandlingException("Required environment variable " + name + " is not set - are you running a function outside of fn run?");
        }
        return val;
    }

    InputEvent readEvent() {
        String callId = env.getOrDefault("FN_CALL_ID", "");
        String deadline = env.get("FN_DEADLINE");
        Instant deadlineDate;

        if (deadline != null) {
            try {
                deadlineDate = Instant.parse(deadline);
            } catch (DateTimeParseException e) {
                throw new FunctionInputHandlingException("Invalid deadline date format", e);
            }
        } else {
            deadlineDate = Instant.now().plus(1, ChronoUnit.HOURS);
        }

        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String lowerCaseKey = entry.getKey().toLowerCase();
            if (lowerCaseKey.startsWith("fn_header_")) {
                headers.put(entry.getKey().substring("fn_header_".length()), entry.getValue());
            }
        }

        return new ReadOnceInputEvent(in, Headers.fromMap(headers), callId, deadlineDate);
    }


    void writeEvent(OutputEvent evt) {
        try {
            evt.writeToOutput(out);
        } catch (IOException e) {
            throw new FunctionOutputHandlingException("error writing event", e);
        }
    }

    @Override
    public void runCodec(Handler h) {
        InputEvent event = readEvent();
        OutputEvent out = h.handle(event);
        writeEvent(out);
    }
}
