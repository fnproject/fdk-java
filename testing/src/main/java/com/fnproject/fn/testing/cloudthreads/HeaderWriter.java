package com.fnproject.fn.testing.cloudthreads;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created on 27/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
class HeaderWriter {
    final OutputStream os;

    HeaderWriter(OutputStream os) {
        this.os = os;
    }

    void writeHeader(String key, String value) throws IOException {
        os.write((key + ": " + value + "\r\n").getBytes());
    }
}
