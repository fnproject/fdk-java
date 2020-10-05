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

import java.util.EventListener;

/**
 * Listener that will be notified in the event that a function invocation executes successfully or fails.
 * error. Instances of InvocationListener should be registered with {@link InvocationContext}.
 */
public interface InvocationListener extends EventListener {

    /**
     * Notifies this InvocationListener that a function invocation has executed successfully.
     */
    void onSuccess();

    /**
     * Notifies this InvocationListener that a function invocation has failed during its execution.
     */
    void onFailure();

}
