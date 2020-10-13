#!/bin/bash
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

