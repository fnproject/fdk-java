#!/bin/sh
if [ -z "$1" ]
then
    echo "Needs runtime folder as an argument"
    exit 1
fi
native_version=$(cat native.version)
set -e

native_image="fnproject/fn-java-native:${native_version}"
if docker pull ${native_image} ; then
   echo ${native_image} already exists, skipping native build
   exit 0
fi
workdir=${1}
dockerfiledir=$(pwd)
(
  cd ${workdir}
  docker build -f ${dockerfiledir}/Dockerfile -t "fnproject/fn-java-native:${native_version}" .
)
echo "fnproject/fn-java-native:${native_version}" > native_build.image
