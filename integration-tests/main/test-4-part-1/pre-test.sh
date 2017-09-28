#!/usr/bin/env bash

set -ex

cp -r ../src-for-test-4/src ./src
cp -r ../src-for-test-4/config ./
cp -r ../src-for-test-4/expected.sh ./
cp -r ../src-for-test-4/func.yaml ./
cp -r ../src-for-test-4/pom.xml ./
cp ../src-for-test-4/Dockerfile.custom ./Dockerfile
