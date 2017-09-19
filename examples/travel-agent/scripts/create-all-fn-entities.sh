#! /usr/bin/env bash

DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

fn apps create travel

fn apps config set travel COMPLETER_BASE_URL "http://$DOCKER_LOCALHOST:8081"

fn routes config set travel /flight/book FLIGHT_API_URL "http://$DOCKER_LOCALHOST:3001/flight"
fn routes config set travel /flight/book FLIGHT_API_SECRET "shhhh"

fn routes config set travel /flight/cancel FLIGHT_API_URL "http://$DOCKER_LOCALHOST:3001/flight"
fn routes config set travel /flight/cancel FLIGHT_API_SECRET "shhhh"

fn routes config set travel /hotel/book HOTEL_API_URL "http://$DOCKER_LOCALHOST:3001/hotel"
fn routes config set travel /hotel/cancel HOTEL_API_URL "http://$DOCKER_LOCALHOST:3001/hotel"

fn routes config set travel /car/book CAR_API_URL "http://$DOCKER_LOCALHOST:3001/car"
fn routes config set travel /car/cancel CAR_API_URL "http://$DOCKER_LOCALHOST:3001/car"

fn routes config set travel /email EMAIL_API_URL "http://$DOCKER_LOCALHOST:3001/email"
