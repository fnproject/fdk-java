#!/usr/bin/env bash

set -ex

rm -rf Dockerfile func.yaml pom.xml src
fn init --runtime=java --name app/test-3
