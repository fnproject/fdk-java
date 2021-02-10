#!/usr/bin/env bash

set -ex

docker version || true
sudo service docker stop || true
curl -fsSL https://get.docker.com/ | sudo sh
# required for image squashing
# sudo cat /etc/docker/daemon.json
echo '{"experimental":true}'  | sudo tee /etc/docker/daemon.json
sudo service docker restart

docker version
