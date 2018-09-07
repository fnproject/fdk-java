package com.fnproject.fn.api;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a set of String-String[] header attributes, per HTTP headers.
 * <p>
 * <p>
 * Headers are immutable
 * <p>
 * keys are are stored  and compared in a case-insensitive way
 */
public final class Headers {
    private static final Headers emptyHeaders = new Headers(Collections.emptyMap());
    private Map<String, List<String>> headers;

    private Headers(Map<String, List<String>> headersIn) {
        this.headers = headersIn;
    }


    /**
     * Build a headers object from a map composed of (name, value) entries, we take a copy of the map and
     * disallow any further modification
     *
     * @param headers underlying collection of header entries to copy
     * @return {@code Headers} built from headers map
     */
    public static Headers fromMap(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headersIn");
        Map<String, List<String>> h = new HashMap<>();
        headers.forEach((k, v) -> h.put(k.toLowerCase(), Collections.singletonList(v)));
        return new Headers(Collections.unmodifiableMap(new HashMap<>(h)));
    }

    /**
     * Build a headers object from a map composed of (name, value) entries, we take a copy of the map and
     * disallow any further modification
     *
     * @param headers underlying collection of header entries to copy
     * @return {@code Headers} built from headers map
     */
    public static Headers fromMultiHeaderMap(Map<String, List<String>> headers) {
        Map<String, List<String>> hm = new HashMap<>();

        headers.forEach((k, vs) -> {
            hm.put(k.toLowerCase(), new ArrayList<>(vs));
        });
        return new Headers(Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(headers))));
    }

    /**
     * Build an empty collection of headers
     *
     * @return empty headers
     */
    public static Headers emptyHeaders() {
        return emptyHeaders;
    }


    /**
     * Creates a new headers object with the specified header added - if a  header with the same key existed it the new value is appended
     * <p>
     * This will overwrite an existing header with an exact name match
     *
     * @param key new header key
     * @param v1  new header value
     * @param vs  additional header values to set
     * @return a new headers object with the specified header added
     */
    public Headers addHeader(String key, String v1, String... vs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key, "value");

        String canonKey = key.toLowerCase();

        Map<String, List<String>> nm = new HashMap<>(headers);
        List<String> current = nm.get(canonKey);

        if (current == null) {
            List<String> s = new ArrayList<>();
            s.add(v1);
            s.addAll(Arrays.asList(vs));

            nm.put(canonKey, Collections.unmodifiableList(s));
        } else {
            List<String> s = new ArrayList<>(current);
            s.add(v1);
            s.addAll(Arrays.asList(vs));
            nm.put(canonKey, Collections.unmodifiableList(s));
        }
        return new Headers(nm);

    }

    /**
     * Creates a new headers object with the specified header set - this overwrites any existin values
     * <p>
     * This will overwrite an existing header with an exact name match
     *
     * @param key new header key
     * @param v1  new header value
     * @param vs  more header values to set
     * @return a new headers object with the specified header added
     */
    public Headers setHeader(String key, String v1, String... vs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(v1, "v1");
        Stream.of(vs).forEach((v) -> Objects.requireNonNull(v, "vs"));

        Map<String, List<String>> nm = new HashMap<>(headers);
        List<String> s = new ArrayList<>();
        s.add(v1);
        s.addAll(Arrays.asList(vs));
        nm.put(key.toLowerCase(), Collections.unmodifiableList(s));
        return new Headers(Collections.unmodifiableMap(nm));
    }


    /**
     * Creates a new headers object with the specified headers set - this overwrites any existin values
     * <p>
     * This will overwrite an existing header with an exact name match
     *
     * @param key new header key
     * @param vs  header values to set
     * @return a new headers object with the specified header added
     */
    public Headers setHeader(String key, Collection<String> vs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(vs, "vs");
        if (vs.size() == 0) {
            throw new IllegalArgumentException("can't set keys to an empty list");
        }
        vs.forEach((v) -> Objects.requireNonNull(v, "vs"));

        Map<String, List<String>> nm = new HashMap<>(headers);
        nm.put(key.toLowerCase(), Collections.unmodifiableList(new ArrayList<>(vs)));
        return new Headers(Collections.unmodifiableMap(nm));

    }

    /**
     * Creates a new headers object with the specified headers remove - this overwrites any existin values
     * <p>
     * This will overwrite an existing header with an exact name match
     *
     * @param key new header key
     * @return a new headers object with the specified header removed
     */
    public Headers removeHeader(String key) {
        Objects.requireNonNull(key, "key");

        if (!headers.containsKey(key.toLowerCase())) {
            return this;
        }

        Map<String, List<String>> nm = new HashMap<>(headers);
        nm.remove(key.toLowerCase());
        return new Headers(Collections.unmodifiableMap(nm));

    }

    /**
     * Returns the header matching the specified key. This matches headers in a case-insensitive way and substitutes
     * underscore and hyphen characters such that : "CONTENT_TYPE" and "Content-type" are equivalent. If no matching
     * header is found then {@code Optional.empty} is returned.
     * <p>
     * When multiple headers are present then the first value is returned- see { #getAllValues(String key)} to get all values for a header
     *
     * @param key match key
     * @return a header matching key or empty if no header matches.
     * @throws NullPointerException if {@code key} is null.
     */
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        String canonKey = key.toLowerCase();

        return headers.entrySet().stream()
           .filter((e) -> e.getKey()
              .equals(canonKey))
           .map(Map.Entry::getValue)
           .map((v) -> v.get(0))
           .findFirst();
    }

    /**
     * Returns a collection  of current header keys
     *
     * @return a collection of keys
     */
    public Collection<String> keys() {
        return headers.keySet();
    }

    /**
     * Returns the headers as a  map
     *
     * @return a map of key->values
     */
    public Map<String, List<String>> asMap() {
        return headers;
    }

    public List<String> getAllValues(String key) {
        return headers.getOrDefault(key, Collections.emptyList());
    }

    public int hashCode() {
        return headers.hashCode();
    }


    public boolean equals(Object other) {
        if (!(other instanceof Headers)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return headers.equals(((Headers) other).headers);
    }

    @Override
    public String toString() {
        return Objects.toString(headers);
    }

}
