#!/usr/bin/env bash

set -ex

docker version || true
sudo service docker stop || true
curl -fsSL https://get.docker.com/ | sudo sh
docker version
