#!/usr/bin/env bash

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
  ./docker-build.sh -t fnproject/fn-java-fdk-build:${BUILD_VERSION} .
)

(
  cd images/build
  ./docker-build.sh -f Dockerfile-jdk11 -t fnproject/fn-java-fdk-build:jdk11-${BUILD_VERSION} .
)

(
   cd runtime
   docker build -t fnproject/fn-java-fdk:${BUILD_VERSION}  -f ../images/runtime/Dockerfile .
)

(
   cd runtime
   docker build -f ../images/runtime/Dockerfile-jre11 -t fnproject/fn-java-fdk:jre11-${BUILD_VERSION} .
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