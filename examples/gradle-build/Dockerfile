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

FROM gradle:4.5.1-jdk8 as build-stage
WORKDIR /function
# needed for gradle?
USER root
ENV GRADLE_USER_HOME /function/.gradle

# Code build
# Copy any build files into the build container and build
COPY *.gradle /function/
RUN ["gradle", "-s", "--no-daemon","--console","plain","cacheDeps"]

# Copies build source into build container
COPY src /function/src

RUN ["gradle", "-s", "--no-daemon","--console","plain","build"]
# Container build
FROM fnproject/fn-java-fdk:1.0.56
WORKDIR /function
COPY --from=build-stage /function/build/libs/*.jar /function/build/deps/*.jar /function/app/
CMD ["com.example.fn.HelloFunction::handleRequest"]
