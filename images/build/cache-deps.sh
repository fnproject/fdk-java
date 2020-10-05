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


set -x
set -e

if [ -n "$FN_REPO_URL" ]; then
  REPO_DFN="-Dfn.repo.url=$FN_REPO_URL"
fi


cd /tmp/cache-deps && mvn test package dependency:copy-dependencies -Dmaven.repo.local=/usr/share/maven/ref/repository -Dmdep.prependGroupId=true -DoutputDirectory=target $REPO_DFN
cd / && rm -fr /tmp/cache-deps
