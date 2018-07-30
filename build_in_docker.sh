#!/usr/bin/env bash
docker run --rm --name compile -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven svenruppert/maven-3.5-jdk-08 mvn clean install
