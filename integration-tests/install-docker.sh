#!/usr/bin/env bash

source "$(dirname "$0")/lib.sh"

set -ex

docker version || true
sudo service docker stop || true
curl -fsSL https://get.docker.com/ | sudo sh
docker version
