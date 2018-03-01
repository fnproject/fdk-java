#!/bin/bash -ex

cd /tmp/staging-repository && python -mSimpleHTTPServer 18080 1>>/tmp/http-logs 2>&1 &
SRV_PROCESS=$!

if [ -n "$DOCKER_LOCALHOST" ]; then
  REPO_ENV="--build-arg FN_REPO_URL=http://$DOCKER_LOCALHOST:18080"
fi

docker build $REPO_ENV $*

kill $SRV_PROCESS
