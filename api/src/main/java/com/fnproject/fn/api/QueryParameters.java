package com.fnproject.fn.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wrapper for query parameters map parsed from the URL of a function invocation.
 */
public interface QueryParameters  {
    /**
     * Find the first entry for {@code key} if it exists otherwise returns {@code Optional.empty}
     *
     * @param key the header entry key (e.g. {@code Accept} in  {@code Accept: text/html}
     * @return the first entry for {@code key} if it was present in the URL otherwise {@code Optional.empty}.
     * @throws NullPointerException if {@code key} is null.
     */
    Optional<String> get(String key);

    /**
     * Find the list of entries for a specific key. Returns {@code Optional.empty}. Useful when multiple values are
     * passed. e.g. this method returns {@code ["val1", "val2", "val3]} for the key {@code param} in the URL
     * {@code http://example.com?param=val1&amp;param=val2&amp;param=val3}
     *
     * @param key the header entry key (e.g. {@code Accept} in {@code Accept: text/html}
     * @return the list of entries for the key if present otherwise the empty list
     * if not present. If the key was present without a value, it returns
     * a list with a single element, the empty string.
     * @throws NullPointerException if {@code key} is null.
     */
    List<String> getValues(String key);

    /**
     * @return a copy of the underlying map storing all query parameters
     */
    Map<String, List<String>> getAll();
}
