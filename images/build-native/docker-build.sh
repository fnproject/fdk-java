#!/bin/sh
native_version=$(cat native.version)
set -e

native_image="fnproject/fn-java-native:${native_version}"
if docker pull ${native_image} ; then
   echo ${native_image} already exists, skipping native build
   exit 0
fi

docker build -t "fnproject/fn-java-native:${native_version}" .
echo "fnproject/fn-java-native:${native_version}" > native_build.image
