#!/bin/bash -ex
if [ -z ${REPOSITORY_LOCATION} ] ; then
 echo no REPOSITORY_LOCATION set
 exit 1;
fi

docker rm -f fn_mvn_repo || true
docker run -d \
            -v "${REPOSITORY_LOCATION}":/repo:ro \
            -w /repo \
            --name fn_mvn_repo \
            python:2.7 \
            python -mSimpleHTTPServer 18080


DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' fn_mvn_repo)
REPO_ENV="--build-arg FN_REPO_URL=http://${DOCKER_LOCALHOST}:18080"

docker build $REPO_ENV $*

docker rm -f fn_mvn_repo