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

if [ -z "$1" ]
then
    echo "Needs runtime folder as an argument"
    exit 1
fi

set -ex

# If not defined, read the FDK and GraalVM versions
if [ -z "${FN_FDK_VERSION}" ];  then
    FN_FDK_VERSION=$(cat ../../release.version)
fi

if [ -z "${GRAALVM_VERSION}" ];  then
    GRAALVM_VERSION=$(cat ../../graalvm.version)
fi

# The path to the FDK runtime root folder
fdk_runtime_root=${1}

dockerfiledir=$(pwd)

# Build JDK 8
native_image="fnproject/fn-java-native:${FN_FDK_VERSION}"
if docker pull ${native_image} ; then
    echo ${native_image} already exists, skipping native build
else
    (
      cd ${dockerfiledir}
      # Make a copy of the Java 8 Dockerfile and update the GraalVM image tag
      cp Dockerfile Dockerfile.build
      sed -i.bak -e "s|##GRAALVM_VERSION##|${GRAALVM_VERSION}|" Dockerfile.build && rm Dockerfile.build.bak
      # Build the Dockerfile in the runtime root--it pulls in needed libs
      cd ${fdk_runtime_root}
      docker build -f ${dockerfiledir}/Dockerfile.build -t "${native_image}" .
      rm ${dockerfiledir}/Dockerfile.build
    )
    echo "${native_image}" > native_build.image
fi

# Build JDK 11
native_image="fnproject/fn-java-native:jdk11-${FN_FDK_VERSION}"
if docker pull ${native_image} ; then
   echo ${native_image} already exists, skipping native build
else
    (
      cd ${dockerfiledir}
      # Make a copy of the Java 11 Dockerfile and update the GraalVM image tag
      cp Dockerfile-jdk11 Dockerfile-jdk11.build
      sed -i.bak -e "s|##GRAALVM_VERSION##|${GRAALVM_VERSION}|" Dockerfile-jdk11.build && rm Dockerfile-jdk11.build.bak
      # Build the Dockerfile in the runtime root--it pulls in needed libs
      cd ${fdk_runtime_root}
      docker build -f ${dockerfiledir}/Dockerfile-jdk11.build -t "${native_image}" .
      rm ${dockerfiledir}/Dockerfile-jdk11.build
    )
    echo "${native_image}" > native_build_11.image
fi
