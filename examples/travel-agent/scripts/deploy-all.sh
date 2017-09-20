#! /usr/bin/env bash

DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

fn apps create travel

for func in "flight/book" "flight/cancel" "hotel/book" "hotel/cancel" "car/book" "car/cancel" "email" "trip"
do
(
  cd $func &&
  fn deploy --app travel --local
)
done