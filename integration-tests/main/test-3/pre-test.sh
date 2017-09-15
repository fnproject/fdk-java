#!/usr/bin/env bash

set -ex

rm -rf Dockerfile func.yaml pom.xml src
FN_JAVA_FDK_VERSION=$(cat ../../../release.version) fn init --runtime=java --name app/test-3
