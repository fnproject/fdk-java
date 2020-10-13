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

set -e

fn --verbose  deploy   --app myapp --local


echo "Calling function"
curl -v -X POST --data-binary @test-image.png -H "Content-type: application/octet-stream" "http://localhost:8080/t/myapp/async-thumbnails"

echo "Contents of bucket"
mc ls -r example-storage-server
echo "Sleeping for 5 seconds to allow flows to complete"
sleep 5
echo "Contents of bucket"
mc ls -r example-storage-server
