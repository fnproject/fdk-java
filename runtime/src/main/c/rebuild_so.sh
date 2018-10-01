#!/usr/bin/env bash


mydir=$(cd "$(dirname "$0")"; pwd)
cd ${mydir}

set -e
docker build -t fdk_c_build -f Dockerfile-buildimage .

docker run  -v $(pwd):/build  fdk_c_build ./buildit.sh
