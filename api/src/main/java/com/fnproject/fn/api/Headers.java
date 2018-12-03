package com.fnproject.fn.api;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Represents a set of String-String[] header attributes, per HTTP headers.
 * <p>
 * Internally header keys are always canonicalized using HTTP header conventions
 * <p>
 * Headers objects are immutable
 * <p>
 * Keys are are stored and compared in a case-insensitive way and are canonicalised according to  RFC 7230 conventions such that  :
 *
 * <ul>
 * <li>a-header</li>
 * <li>A-Header</li>
 * <li>A-HeaDer</li>
 * </ul>
 * are all equivalent - keys are returned in the canonical form (lower cased except for leading characters)
 * Where keys do not comply with HTTP header naming they are left as is.
 */
public final class Headers implements Serializable {
    private static final Headers emptyHeaders = new Headers(Collections.emptyMap());
    private Map<String, List<String>> headers;

    private Headers(Map<String, List<String>> headersIn) {
        this.headers = headersIn;
    }

    private static Pattern headerName = Pattern.compile("[A-Za-z0-9!#%&'*+-.^_`|~]+");

    public Map getAll() {
        return headers;
    }

    /**
     * Calculates the canonical key  (cf RFC 7230) for a header
     * <p>
     * If the header contains invalid characters it returns the original header
     *
     * @param key the header key to canonicalise
     * @return a canonical key or the original key if the input contains invalid character
     */
    public static String canonicalKey(String key) {
        if (!headerName.matcher(key).matches()) {
            return key;
        }
        String parts[] = key.split("-", -1);
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() > 0) {
                parts[i] = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
            }
        }
        return String.join("-", parts);

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
        headers.forEach((k, v) -> h.put(canonicalKey(k), Collections.singletonList(v)));
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

        headers.forEach((k, vs) -> hm.put(canonicalKey(k), new ArrayList<>(vs)));
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
     * Sets a map of headers, overwriting any headers in the current headers with the respective values
     *
     * @param vals a map of headers
     * @return a new headers object with thos headers set
     */
    public Headers setHeaders(Map<String, List<String>> vals) {
        Objects.requireNonNull(vals, "vals");
        Map<String, List<String>> nm = new HashMap<>(headers);
        vals.forEach((k, vs) -> {
            vs.forEach(v -> Objects.requireNonNull(v, "header list contains null entries"));
            nm.put(canonicalKey(k), vs);
        });
        return new Headers(nm);
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

        String canonKey = canonicalKey(key);

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
        nm.put(canonicalKey(key), Collections.unmodifiableList(s));
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
        nm.put(canonicalKey(key), Collections.unmodifiableList(new ArrayList<>(vs)));
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

        String canonKey = canonicalKey(key);
        if (!headers.containsKey(canonKey)) {
            return this;
        }

        Map<String, List<String>> nm = new HashMap<>(headers);
        nm.remove(canonKey);
        return new Headers(Collections.unmodifiableMap(nm));

    }

    /**
     * Returns the header matching the specified key. This matches headers in a case-insensitive way and substitutes
     * underscore and hyphen characters such that : "CONTENT_TYPE_HEADER" and "Content-type" are equivalent. If no matching
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
        String canonKey = canonicalKey(key);

        List<String> val = headers.get(canonKey);
        if (val == null){
            return Optional.empty();
        }
        return Optional.of(val.get(0));
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
     * @return a map of key-values
     */
    public Map<String, List<String>> asMap() {
        return headers;
    }

    /**
     * GetAllValues returns all values for a header or an empty list if the header has no values
     * @param key the Header key
     * @return a possibly empty list of values
     */
    public List<String> getAllValues(String key) {
        return headers.getOrDefault(canonicalKey(key), Collections.emptyList());
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
