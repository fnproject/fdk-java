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

/**
 * This uses maven-resource filtering rather than conventional manifest versioning as it's more robust against resource changes than using standard META-INF/MANIFEST.MF
 * versioning. For native image functions this negates the need for extra configuration to include manifest resources.
 *
 * Created on 18/02/2020.
 * <p>
 *
 * (c) 2020 Oracle Corporation
 */
public class Version {
    public static final String FDK_VERSION="${project.version}";
}
