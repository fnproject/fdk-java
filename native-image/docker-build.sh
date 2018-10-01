#!/bin/sh
docker build -t "fnproject/fn-java-native:latest" .
(
  cd init-image
  ./docker-build.sh
)
