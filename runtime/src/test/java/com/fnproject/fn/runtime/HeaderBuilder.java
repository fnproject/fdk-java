package com.fnproject.fn.runtime;

import java.util.AbstractMap;
import java.util.Map;

class HeaderBuilder {
    static Map.Entry<String, String> headerEntry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
