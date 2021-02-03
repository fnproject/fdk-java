#!/bin/sh
#
# Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ -z "${FN_FDK_VERSION}" ];  then
    FN_FDK_VERSION=$(cat ../../release.version)
fi

set -e

# Update pom.xml with current FDK version
sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${FN_FDK_VERSION}</fdk.version>|" pom.xml && rm pom.xml.bak

# Create Dockerfile with current FDK build tag (Java 8)
cp Dockerfile Dockerfile.build
sed -i.bak -e "s|##FN_FDK_VERSION##|${FN_FDK_VERSION}|" Dockerfile.build && rm Dockerfile.build.bak
cat Dockerfile.build

# Build init image packaging created Dockerfile (Java 8)
docker build -t fnproject/fn-java-native-init:${FN_FDK_VERSION} -f Dockerfile-init-image .

# Create Dockerfile with current FDK build tag (Java 11)
cp Dockerfile Dockerfile.build
sed -i.bak -e "s|##FN_FDK_VERSION##|jdk11-${FN_FDK_VERSION}|" Dockerfile.build && rm Dockerfile.build.bak

# Build init image packaging created Dockerfile (Java 11)
docker build -t fnproject/fn-java-native-init:jdk11-${FN_FDK_VERSION} -f Dockerfile-init-image .
rm Dockerfile.build
