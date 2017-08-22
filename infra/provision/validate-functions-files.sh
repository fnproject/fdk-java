#!/bin/bash
FUNCTIONS_SERVICE_DIR=$1

set -ex

if [[ -z "${FUNCTIONS_SERVICE_DIR// }" ]]; then
    echo "ERROR: must specify the location of the functions-service local repository as the argument"
    exit 2
fi

# This dry run validation is performed so that we don't delete our environment and then fail to recreate it.
kubectl create --dry-run -f $FUNCTIONS_SERVICE_DIR/scripts/docker-reg-secret.yml
kubectl create --dry-run -f $FUNCTIONS_SERVICE_DIR/scripts/zipkin-service.yml
kubectl create --dry-run -f $FUNCTIONS_SERVICE_DIR/scripts/fn-service.yml

