# Building and releasing a new fn-java-fdk base image

## Pre-requisites
1. Maven.
2. [Smith](https://github.com/oracle/smith).
3. Docker.

## Build and publish to Docker Hub

Note: smith needs to be run on Linux.

```sh
mvn clean package
smith
smith upload -r https://<user>:<pass>@registry-1.docker.io/fnproject/fn-java-fdk:micro-jrebase
docker build . -t fnproject/fn-java-fdk
docker push fnproject/fn-java-fdk
```
