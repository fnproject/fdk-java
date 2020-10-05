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

package com.fnproject.fn.api;

import java.io.Closeable;
import java.io.InputStream;
import java.time.Instant;
import java.util.function.Function;

public interface InputEvent extends Closeable {

    /**
     * Consume the body associated with this event
     * <p>
     * This may only be done once per request.
     *
     * @param dest a function to send the body to - this does not need to close the incoming stream
     * @param <T>  An optional return type
     * @return the new
     */
    <T> T consumeBody(Function<InputStream, T> dest);



    /**
     * return the current call ID for this event
     * @return a call ID
     */
    String getCallID();


    /**
     * The deadline by which this event should be processed - this is information and is intended to help you determine how long you should spend processing your event - if you exceed this deadline Fn will terminate your container.
     *
     * @return a deadline relative to the current system clock that the event must be processed by
     */
    Instant getDeadline();


    /**
     * The HTTP headers on the request
     *
     * @return an immutable map of headers
     */
    Headers getHeaders();


}
