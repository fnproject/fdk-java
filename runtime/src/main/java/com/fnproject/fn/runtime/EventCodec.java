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

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;

/**
 * Event Codec - deals with different calling conventions between fn and the function docker container
 */
public interface EventCodec {

    /**
     * Handler handles function content  based on codec events
     * <p>
     * A handler should generally deal with all exceptions (except errors) and convert them into appropriate OutputEvents
     */
    interface Handler {
        /**
         * Handle a function input event and generate a response
         *
         * @param event the event to handle
         * @return an output event indicating the result of calling a function or an error
         */
        OutputEvent handle(InputEvent event);
    }

    /**
     * Run Codec should continuously run the function event loop until either  the FDK should exit normally (returning normally) or an error occurred.
     * <p>
     * Codec should invoke the handler for each received event
     *
     * @param h the handler to run
     */
    void runCodec(Handler h);

}
