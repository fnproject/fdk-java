/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.runtime.httpgateway;

import com.fnproject.fn.api.QueryParameters;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class QueryParametersParser {
    public static QueryParameters getParams(String url) {
        int beginIndex = url.indexOf("?");
        if (beginIndex < 0) {
            return new QueryParametersImpl();
        }
        String queryParameters = url.substring(beginIndex + 1);
        return new QueryParametersImpl(parseQueryParameters(queryParameters));
    }

    private static Map<String, List<String>> parseQueryParameters(String queryParameters) {
        if (queryParameters == null || "".equals(queryParameters)) {
            return Collections.emptyMap();
        }
        return Arrays.stream(queryParameters.split("[&;]"))
                .map(QueryParametersParser::splitQueryParameter)
                .collect(Collectors.groupingBy(Entry::getKey, LinkedHashMap::new, mapping(Entry::getValue, toList())));
    }

    private static Entry<String, String> splitQueryParameter(String parameter) {
        final int idx = parameter.indexOf("=");

        final String key = decode(idx > 0 ? parameter.substring(0, idx) : parameter);
        final String value = idx > 0 && parameter.length() > idx + 1 ? decode(parameter.substring(idx + 1)) : "";
        return new SimpleImmutableEntry<>(key, value);
    }

    private static String decode(String urlEncodedString) {
        try {
            return URLDecoder.decode(urlEncodedString, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("No utf-8 support in underlying platform", e);
        }
    }
}
