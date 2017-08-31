package com.fnproject.fn.api;

import java.util.*;

/**
 * Represents the headers on an HTTP request or response. Multiple headers with the same key are collapsed into a single
 * entry where the values are concatenated by commas as per the HTTP spec (RFC 7230).
 */
public final class Headers {
    private Map<String, String> headers;

    private Headers(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Build a headers object from a map composed of (name, value) entries, we take a copy of the map and
     * disallow any further modification
     *
     * @param headers underlying collection of header entries to copy
     * @return {@code Headers} built from headers map
     */
    public static Headers fromMap(Map<String, String> headers) {
        return new Headers(Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(headers))));
    }

    /**
     * Build an empty collection of headers
     *
     * @return empty headers
     */
    public static Headers emptyHeaders() {
        return new Headers(Collections.emptyMap());
    }

    /**
     * Returns the header matching the specified key. This matches headers in a case-insensitive way and substitutes
     * underscore and hyphen characters such that : "CONTENT_TYPE" and "Content-type" are equivalent. If no matching
     * header is found then {@code Optional.empty} is returned.
     * <p>
     * Multiple headers are collapsed by {@code fn} into a single header entry delimited by commas (see
     * <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC7230 Sec 3.2.2</a> for details), for example
     *
     * <pre>
     *     Accept: text/html
     *     Accept: text/plain
     * </pre>
     *
     * is collapsed into
     *
     * <pre>
     *     Accept: text/html, text/plain
     * </pre>
     *
     * @param key match key
     * @return a header matching key or empty if no header matches.
     * @throws NullPointerException if {@code key} is null.
     */
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return getAll().entrySet().stream()
                .filter((e) -> e.getKey()
                        .replaceAll("-", "_")
                        .equalsIgnoreCase(key.replaceAll("-", "_")))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * The function invocation headers passed on the request
     *
     * @return a map of Invocation headers.
     */
    public Map<String, String> getAll() {
        return headers;
    }
}
