#!/bin/bash

# This hook runs in the test directory immediately prior to any "fn build" invocation.

# Turn these lines in func.yaml:

# name: jbloggs/cloud-thread-function
# version: 0.0.1

# into these:

# name: docker-registry:5000/jbloggs/cloud-thread-function
# version: 4837492387439724389 <- whatever suffix is.

set -e

while read key rest
do
  case "$key" in
  name:)
    rest="docker-registry:5000/$rest"
    ;;
  version:)
    rest="$SUFFIX"
    ;;
  esac
  echo "$key $rest"
done < func.yaml > .func.yaml-new

mv func.yaml .func.yaml-old
mv .func.yaml-new func.yaml
