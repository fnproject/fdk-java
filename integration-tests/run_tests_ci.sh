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
            -w /repo -p18080:18080 \
            --name fn_mvn_repo \
            python:2.7 \
            python -mSimpleHTTPServer 18080

until $(curl --output /dev/null --silent --head --fail http://localhost:18080); do
    printf '.'
    sleep 1
done

# Start Fn
fn stop || true
fn start  --log-level=debug -d

until $(curl --output /dev/null --silent --head --fail http://localhost:8080); do
    printf '.'
    sleep 1
done


export FN_LOG_FILE=/tmp/fn.log
docker logs -f fnserver >& ${FN_LOG_FILE} &
DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' fnserver)

export DOCKER_LOCALHOST
docker rm -f flowserver || true
docker run --rm -d \
      -p 8081:8081 \
      -e API_URL="http://${DOCKER_LOCALHOST}:8080/invoke" \
      -e no_proxy=${DOCKER_LOCALHOST} \
      --name flowserver \
      fnproject/flow:latest

until $(curl --output /dev/null --silent --head --fail http://localhost:8081); do
    printf '.'
    sleep 1
done
export FLOW_LOG_FILE=/tmp/flow.log

docker logs -f flowserver >& ${FLOW_LOG_FILE} &
set +e


mvn --quiet -B  test
result=$?



docker rm -f flowserver
docker rm -f fnserver
docker rm -rf fn_mvn_repo

exit $result
