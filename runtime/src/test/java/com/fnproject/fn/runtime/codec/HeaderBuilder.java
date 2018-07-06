package com.fnproject.fn.runtime.codec;

import java.util.AbstractMap;
import java.util.Map;

public class HeaderBuilder {
    public static Map.Entry<String, String> headerEntry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
