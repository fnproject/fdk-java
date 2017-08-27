package com.fnproject.fn.testing.cloudthreads;

import java.io.IOException;

/**
 * Created on 27/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
interface HeaderWriter {
    void writeHeader(String key, String value) throws IOException;
}
