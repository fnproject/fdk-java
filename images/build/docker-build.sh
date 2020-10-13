#!/bin/bash -ex
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