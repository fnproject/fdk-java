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

package com.example.fn.testing;

import com.example.fn.StringReverse;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class StringReverseTest {
    private StringReverse stringReverse = new StringReverse();

    @Test
    public void reverseEmptyString() {
        assertEquals("", reverse(""));
    }

    @Test
    public void reverseOfSingleCharacter() {
        assertEquals("a", reverse("a"));
    }

    @Test
    public void reverseHelloIsOlleh() {
        assertEquals("olleh", reverse("hello"));
    }

    private String reverse(String str) {
        return stringReverse.reverse(str);
    }
}
