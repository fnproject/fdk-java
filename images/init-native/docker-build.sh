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
sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${FN_FDK_VERSION}</fdk.version>|" pom.xml && rm pom.xml.bak
cp Dockerfile Dockerfile.build
docker build -t fnproject/fn-java-native-init:${FN_FDK_VERSION} -f Dockerfile-init-image .

cp Dockerfile Dockerfile.build
sed -i.bak -e "s|fnproject/fn-java-fdk-build:latest|fnproject/fn-java-fdk-build:jdk11-latest|" Dockerfile.build && rm Dockerfile.build.bak
sed -i.bak -e "s|fnproject/fn-java-native:latest|fnproject/fn-java-native:jdk11-latest|" Dockerfile.build && rm Dockerfile.build.bak
docker build -t fnproject/fn-java-native-init:jdk11-${FN_FDK_VERSION} -f Dockerfile-init-image .
rm Dockerfile.build