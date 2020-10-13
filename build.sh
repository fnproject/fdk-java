#!/usr/bin/env bash
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

set -e
set -x
mkdir -p /tmp/staging_repo
rm -rf /tmp/staging_repo/*

BUILD_VERSION=${FN_FDK_VERSION:-1.0.0-SNAPSHOT}
export REPOSITORY_LOCATION=${REPOSITORY_LOCATION:-/tmp/staging_repo}

while [ $# -ne 0 ]
do
  case "$1" in
        --build-native-java)
            BUILD_NATIVE_JAVA=true
            ;;
  esac
  shift
done

(
    runtime/src/main/c/rebuild_so.sh
)

mvn  -B  deploy -DaltDeploymentRepository=localStagingDir::default::file://${REPOSITORY_LOCATION}

(
  cd images/build
  ./docker-build.sh --no-cache -t fnproject/fn-java-fdk-build:${BUILD_VERSION} .
)

(
  cd images/build
  ./docker-build.sh --no-cache -f Dockerfile-jdk11 -t fnproject/fn-java-fdk-build:jdk11-${BUILD_VERSION} .
)

(
   cd runtime
   docker build --no-cache -t fnproject/fn-java-fdk:${BUILD_VERSION}  -f ../images/runtime/Dockerfile .
)

(
   cd runtime
   docker build --no-cache -f ../images/runtime/Dockerfile-jre11 -t fnproject/fn-java-fdk:jre11-${BUILD_VERSION} .
)

(
    workdir=$(pwd)/runtime
    cd images/build-native
    ./docker-build.sh ${workdir}
)

(
    cd images/init-native
    ./docker-build.sh
)
