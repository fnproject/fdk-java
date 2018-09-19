package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class HeaderBuilder {
    static Map.Entry<String, List<String>> headerEntry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(Headers.canonicalKey(key), Collections.singletonList(value));
    }
}
