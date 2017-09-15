package com.fnproject.fn.runtime.flow;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Both an HTTP response and an individual part of a multipart MIME stream are constituted of
 * a set of headers together with the body stream. This interface abstracts the access to those parts.
 */
interface EntityReader {
    String getHeaderElement(String h, String e);

    Optional<String> getHeaderValue(String header);

    InputStream getContentStream();

    Map<String, String> getHeaders();
}
