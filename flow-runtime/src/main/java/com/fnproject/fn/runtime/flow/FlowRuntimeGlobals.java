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

package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Optional;

/**
 * Globals for injecting testing and global state into flow
 */
public class FlowRuntimeGlobals {
    private static CompleterClientFactory completerClientFactory = null;
    private static CompletionId currentCompletionId;

    private static ObjectMapper objectMapper;


    /**
     * Get the default object mapper to use for Flow Invocations
     *
     * this will return a runtime-local
     * @return an initialized objectmapper
     */
    public synchronized static ObjectMapper getObjectMapper(){
        if(objectMapper == null){
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    /**
     * Get the current completion ID  - returns an empty optional if the current invocation is not a completion
     *
     * @return a completion ID option (empty of if not in a completion
     */
    public static Optional<CompletionId> getCurrentCompletionId() {
        return Optional.ofNullable(currentCompletionId);
    }

    /**
     * Set the current Completion ID
     * @param completionId a completion ID - set to null to clear the current completion ID
     */
    public static void setCurrentCompletionId(CompletionId completionId) {
        currentCompletionId = completionId;
    }

    /**
     * Resets the state of the completer client factory to defaults; this is primarily for testing
     */
    public static void resetCompleterClientFactory() {
        completerClientFactory = null;
    }

    /**
     * override the default completer client factory
     *
     * @param currentClientFactory a new client factory to be used to create flows
     */
    public static void setCompleterClientFactory(CompleterClientFactory currentClientFactory) {
        completerClientFactory = Objects.requireNonNull(currentClientFactory);
    }

    /**
     * return the current Fn Flow client factory;
     *
     * @return
     */
    public static CompleterClientFactory getCompleterClientFactory() {
        return completerClientFactory;
    }
}
