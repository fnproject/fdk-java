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

package com.fnproject.fn.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexQuery {
    public Response query(Query query) {
        return new Response(query.regex, query.text, getMatches(query));
    }

    private List<Match> getMatches(Query query) {
        Pattern pattern = Pattern.compile(query.regex);
        Matcher matcher = pattern.matcher(query.text);
        List<Match> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new Match(matcher.start(), matcher.end(), query.text.substring(matcher.start(), matcher.end())));
        }
        return matches;
    }
}
