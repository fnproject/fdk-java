#!/bin/bash

# This hook runs in the test directory immediately prior to any "fn build" invocation.

# Turn these lines in func.yaml:

# name: jbloggs/cloud-thread-function
# version: 0.0.1

# into these:

# name: docker-registry:5000/jbloggs/cloud-thread-function
# version: 4837492387439724389 <- whatever suffix is.

set -ex

docker push $(awk '/^name:/ { print $2 }' func.yaml):$SUFFIX
mv .func.yaml-old func.yaml
