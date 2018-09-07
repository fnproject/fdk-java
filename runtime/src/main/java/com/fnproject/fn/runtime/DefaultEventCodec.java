package com.fnproject.fn.runtime;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.exception.FunctionInputHandlingException;
import com.fnproject.fn.api.exception.FunctionOutputHandlingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.*;

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

    public DefaultEventCodec(Map<String, String> env, InputStream in, OutputStream out) {
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

    protected InputEvent readEvent() {
        String method = getRequiredEnv("FN_METHOD");
        String appName = getRequiredEnv("FN_APP_NAME");
        String route = getRequiredEnv("FN_PATH");
        String requestUrl = getRequiredEnv("FN_REQUEST_URL");
        String callId = getRequiredEnv("FN_CALL_ID");
        String deadline = getRequiredEnv("FN_DEADLINE");
        Date deadlineDate;
        try {
            deadlineDate = StdDateFormat.getISO8601Format(TimeZone.getDefault(), Locale.getDefault()).parse(deadline);
        } catch (ParseException e) {
            throw new FunctionInputHandlingException("Invalid deadline date format", e);
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


    protected void writeEvent(OutputEvent evt) {
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
