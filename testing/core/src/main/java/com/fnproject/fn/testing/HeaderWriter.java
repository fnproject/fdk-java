package com.fnproject.fn.testing;

import java.io.IOException;
import java.io.OutputStream;

class HeaderWriter {
    final OutputStream os;

    HeaderWriter(OutputStream os) {
        this.os = os;
    }

    void writeHeader(String key, String value) throws IOException {
        os.write((key + ": " + value + "\r\n").getBytes("ISO-8859-1"));
    }
}
