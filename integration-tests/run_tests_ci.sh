#!/usr/bin/env bash
set -e
set -x


cd $(dirname $0)

if [ -z ${REPOSITORY_LOCATION} ]; then
   echo no REPOSITORY_LOCATION is specified in env - you need to deploy fn jars to a local dir
   exit 1
fi


docker rm -f fn_mvn_repo || true
docker run -d \
            -v "$REPOSITORY_LOCATION":/repo:ro \
            -w /repo \
            --name fn_mvn_repo \
            python:2.7 \
            python -mSimpleHTTPServer 18080


# Start Fn
fn stop || true
fn start  --log-level=debug -d
echo $?;


export FN_LOG_FILE=/tmp/fn.log

docker logs -f fnserver >& ${FN_LOG_FILE} &

DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' fnserver)

docker rm -f flowserver || true
docker run --rm -d \
      -p 8081:8081 \
      -e API_URL="http://${DOCKER_LOCALHOST}:8080/invoke" \
      -e no_proxy=${DOCKER_LOCALHOST} \
      --name flowserver \
      fnproject/flow:latest

export FLOW_LOG_FILE=/tmp/flow.log

docker logs -f flowserver >& ${FLOW_LOG_FILE} &
set +e


mvn -B  test
result=$?



docker rm -f flowserver
docker rm -f fnserver
docker rm -rf fn_mvn_repo

exit $result