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
native_version=$(cat native.version)
set -e

workdir=${1}
dockerfiledir=$(pwd)

# Build JDK 8
native_image="fnproject/fn-java-native:${native_version}"
if docker pull ${native_image} ; then
    echo ${native_image} already exists, skipping native build
else
    (
      cd ${workdir}
      docker build -f ${dockerfiledir}/Dockerfile -t "${native_image}" .
    )
    echo "${native_image}" > native_build.image
fi

# Build JDK 11
native_image="fnproject/fn-java-native:jdk11-${native_version}"
if docker pull ${native_image} ; then
   echo ${native_image} already exists, skipping native build
else
    (
      cd ${workdir}
      docker build -f ${dockerfiledir}/Dockerfile-jdk11 -t "${native_image}" .
    )
    echo "${native_image}" > native_build_11.image
fi
