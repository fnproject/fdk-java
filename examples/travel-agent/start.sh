#! /usr/bin/env bash

(fn start > /dev/null 2>&1 &)

sleep 5

DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

docker run --rm  \
       -p 8081:8081 \
       -d \
       -e API_URL="http://$DOCKER_LOCALHOST:8080/r" \
       -e no_proxy=$DOCKER_LOCALHOST \
       --name completer \
       fnproject/completer:latest

docker run --rm \
       -p 3000:3000 \
       -p 3001:3001 \
       -d \
       --name bristol \
       tteggel/bristol

docker run --rm \
       -p 3002:3000 \
       -d \
       --name flowui \
       -e API_URL=http://$DOCKER_LOCALHOST:8080 \
       -e COMPLETER_BASE_URL=http://$DOCKER_LOCALHOST:8081 \
       fnproject/completer:ui

fn apps create travel

fn apps config set travel COMPLETER_BASE_URL "http://$DOCKER_LOCALHOST:8081"

(cd flight/book \
  && fn deploy --local --app travel \
  && fn routes config set travel /flight/book AIRLINE_API_URL "http://$DOCKER_LOCALHOST:3001/flight" \
  && fn routes config set travel /flight/book AIRLINE_API_SECRET "shhhh"
)

(cd hotel/book \
  && fn deploy --local --app travel \
  && fn routes config set travel /hotel/book HOTEL_API_URL "http://$DOCKER_LOCALHOST:3001/hotel"
)

(cd car/book \
  && fn deploy --local --app travel \
  && fn routes config set travel /car/book CAR_API_URL "http://$DOCKER_LOCALHOST:3001/car"
)
