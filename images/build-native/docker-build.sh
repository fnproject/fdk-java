#!/bin/sh
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
