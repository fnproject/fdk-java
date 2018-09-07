package com.fnproject.fn.runtime;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class HeaderBuilder {
    static Map.Entry<String, List<String>> headerEntry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, Collections.singletonList(value));
    }
}
