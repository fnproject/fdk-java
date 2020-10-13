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

import com.fnproject.fn.testing.FnTestingRule;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class RegexQueryTests {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    @Test
    public void matchingSingleCharacter() throws JSONException {
        String text = "a";
        String regex = ".";
        fn.givenEvent()
                .withBody(String.format("{\"text\": \"%s\", \"regex\": \"%s\"}", text, regex))
                .enqueue();

        fn.thenRun(RegexQuery.class, "query");

        JSONAssert.assertEquals(String.format("{\"text\":\"%s\"," +
                        "\"regex\":\"%s\"," +
                        "\"matches\":[" +
                        "{\"start\": 0, \"end\": 1, \"match\": \"a\"}" +
                        "]}", text, regex),
                fn.getOnlyResult().getBodyAsString(), false);
    }

    @Test
    public void matchingSingleCharacterMultipleTimes() throws JSONException {
        String text = "abc";
        String regex = ".";
        fn.givenEvent()
                .withBody(String.format("{\"text\": \"%s\", \"regex\": \"%s\"}", text, regex))
                .enqueue();

        fn.thenRun(RegexQuery.class, "query");

        JSONAssert.assertEquals(String.format("{\"text\":\"%s\"," +
                        "\"regex\":\"%s\"," +
                        "\"matches\":[" +
                        "{\"start\": 0, \"end\": 1, \"match\": \"a\"}," +
                        "{\"start\": 1, \"end\": 2, \"match\": \"b\"}," +
                        "{\"start\": 2, \"end\": 3, \"match\": \"c\"}" +
                        "]}", text, regex),
                fn.getOnlyResult().getBodyAsString(), false);
    }
}
