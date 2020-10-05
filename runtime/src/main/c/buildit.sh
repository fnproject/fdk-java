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

src_dir=$(pwd)
build_dir=${src_dir}/build/$(uname -s| tr '[:upper:]' '[:lower:]')

mkdir -p ${build_dir}
(
    cd  ${build_dir}
    cmake ${src_dir}

    make
)
mv ${build_dir}/libfnunixsocket.* ${src_dir}