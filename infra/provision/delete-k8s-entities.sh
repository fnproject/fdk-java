#!/bin/bash
PROVISION_DIR=$1
FUNCTIONS_SERVICE_DIR=$2

set -ex

if [[ -z "${PROVISION_DIR// }" ]]; then
    echo "ERROR: must specify the jfaas/infra/provision directory as the first argument"
    exit 2
fi
if [[ -z "${FUNCTIONS_SERVICE_DIR// }" ]]; then
    echo "ERROR: must specify the location of the functions-service local repository as the second argument"
    exit 2
fi

kubectl delete -f $PROVISION_DIR/completer-integration-environment.yaml || true
kubectl delete -f $PROVISION_DIR/registry.yaml || true

kubectl delete -f $FUNCTIONS_SERVICE_DIR/scripts/fn-service.yml || true
kubectl delete -f $FUNCTIONS_SERVICE_DIR/scripts/zipkin-service.yml || true
kubectl delete -f $FUNCTIONS_SERVICE_DIR/scripts/docker-reg-secret.yml || true
