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

package com.fnproject.fn.runtime;

import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.runtime.httpgateway.QueryParametersParser;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class QueryParametersParserTest {
    @Test
    public void noUrlParametersProducesEmptyMap() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com");

        assertThat(params.getAll().size()).isEqualTo(0);
    }

    @Test
    public void gettingNonExistentParameterProducesOptionalEmpty() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com");

        assertThat(params.getValues("var")).isEmpty();
    }

    @Test
    public void singleEmptyParameter() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/route?var");

        assertThat(params.getAll().size()).isEqualTo(1);
        assertThat(params.get("var")).hasValue("");
    }

    @Test
    public void multipleEmptyParameters() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/myapp/route?var&var2");

        assertThat(params.getAll().size()).isEqualTo(2);
        assertThat(params.get("var")).hasValue("");
        assertThat(params.get("var2")).hasValue("");
    }

    @Test
    public void singleParameterWithValue() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/myapp/route?var=val");

        assertThat(params.getAll().size()).isEqualTo(1);
        assertThat(params.get("var")).hasValue("val");
    }

    @Test
    public void multipleParametersWithValues() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/myapp/route?var1=val1&var2=val2");

        assertThat(params.getAll().size()).isEqualTo(2);
        assertThat(params.get("var1")).hasValue("val1");
        assertThat(params.get("var2")).hasValue("val2");
    }

    @Test
    public void singleParameterWithMultipleValues() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/myapp/route?var=val1&var=val2");

        assertThat(params.getAll().size()).isEqualTo(1);
        assertThat(params.getValues("var")).containsOnly("val1", "val2");
    }

    @Test
    public void multipleParametersMultipleValues() {
        QueryParameters params = QueryParametersParser.getParams("www.example.com/myapp/route?var=val1&var=val2&var2=val&var2=val2");

        assertThat(params.getAll().size()).isEqualTo(2);
        assertThat(params.getValues("var")).containsOnly("val1", "val2");
        assertThat(params.getValues("var2")).containsOnly("val", "val2");
    }

    @Test
    public void urlEncodedParameterWithUrlEncodedValue() {
        QueryParameters params = QueryParametersParser.getParams("/myapp/route?var+blah=val+foo%26%3D%3d");

        assertThat(params.getAll().size()).isEqualTo(1);
        assertThat(params.getValues("var blah")).containsOnly("val foo&==");
    }

    @Test
    public void colonSeparatedParameters() {
        QueryParameters params = QueryParametersParser.getParams("/myapp/route?var=val;var2=val;var=val2");

        assertThat(params.getAll().size()).isEqualTo(2);
        assertThat(params.getValues("var")).containsOnly("val", "val2");
        assertThat(params.get("var2")).hasValue("val");
    }

    @Test
    public void enumeratingQueryParameters() {
        QueryParameters params = QueryParametersParser.getParams("/myapp/route?var1=val1&var2=val2&var3=val3");
        Map<String, List<String>> expectedParams = new HashMap<>();
        expectedParams.put("var1", Collections.singletonList("val1"));
        expectedParams.put("var2", Collections.singletonList("val2"));
        expectedParams.put("var3", Collections.singletonList("val3"));

        for (Map.Entry<String, List<String>> entry : params.getAll().entrySet()) {
            assertThat(entry.getValue()).isEqualTo(expectedParams.get(entry.getKey()));
        }
    }
}

