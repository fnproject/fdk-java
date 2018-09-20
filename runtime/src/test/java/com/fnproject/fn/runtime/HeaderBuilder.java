package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;

import java.util.*;

class HeaderBuilder {
    static Map.Entry<String, List<String>> headerEntry(String key, String... values) {
        return new AbstractMap.SimpleEntry<>(Headers.canonicalKey(key), Arrays.asList(values));
    }
}
