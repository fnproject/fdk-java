#!/usr/bin/env bash
#
# Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e
set -x


cd $(dirname $0)

if [ -z ${REPOSITORY_LOCATION} ]; then
   echo no REPOSITORY_LOCATION is specified in env  using /tmp/staging_repo as default

   REPOSITORY_LOCATION=/tmp/staging_repo
fi


docker rm -f fn_mvn_repo || true
docker run -d \
            -v "${REPOSITORY_LOCATION}":/repo:ro \
            -w /repo -p18080:18080 \
            --name fn_mvn_repo \
            python:2.7 \
            python -mSimpleHTTPServer 18080


until $(curl --output /dev/null --silent  --fail http://localhost:18080); do
    printf '.'
    sleep 1
done


# Start Fn
fn stop || true
fn start  --log-level=debug -d --env-file fnserver.env

until $(curl --output /dev/null --silent --fail http://localhost:8080/); do
    printf '.'
    sleep 1
done


export FN_LOG_FILE=/tmp/fn.log
docker logs -f fnserver >& ${FN_LOG_FILE} &

FNSERVER_IP=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' fnserver)



docker rm -f flowserver || true
docker run --rm -d \
      -p 8081:8081 \
      -e API_URL="http://${FNSERVER_IP}:8080/invoke" \
      -e no_proxy=${FNSERVER_IP} \
      --name flowserver \
      fnproject/flow:latest

until $(curl --output /dev/null --silent --fail http://localhost:8081/ping); do
    printf '.'
    sleep 1
done
export FLOW_LOG_FILE=/tmp/flow.log

docker logs -f flowserver >& ${FLOW_LOG_FILE} &
set +e


if [ $(uname -s) == "Darwin" ] ; then
   DOCKER_LOCALHOST=docker.for.mac.host.internal
else
   DOCKER_LOCALHOST=$(ifconfig eth0| grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')
fi

export DOCKER_LOCALHOST

REPO_IP=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' fn_mvn_repo)
MAVEN_REPOSITORY="http://${REPO_IP}:18080"
export MAVEN_REPOSITORY
COMPLETER_IP=$(docker inspect --type container -f '{{.NetworkSettings.IPAddress}}' flowserver)
COMPLETER_BASE_URL="http://${COMPLETER_IP}:8081"
export COMPLETER_BASE_URL

export no_proxy="${no_proxy},${DOCKER_LOCALHOST},${COMPLETER_IP},${REPO_IP}"




echo "Running tests"
mvn   test
result=$?



docker rm -f flowserver
docker rm -f fnserver
docker rm -rf fn_mvn_repo

exit $result
