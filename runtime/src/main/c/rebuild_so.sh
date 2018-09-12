#!/usr/bin/env bash


mydir=$(realpath $(dirname $0))

cd $mydir

set -e
docker build -t fdk_c_build -f Dockerfile-buildimage .

docker run  -v $(pwd):/build  fdk_c_build cmake .
docker run  -v $(pwd):/build  fdk_c_build make