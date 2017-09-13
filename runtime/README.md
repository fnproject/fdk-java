# Building and releasing a new fn-java-fdk base image

## Pre-requisites
1. Maven
2. [Smith](https://github.com/oracle/smith).
3. Docker (obvs).

## Build and publish to Docker Hub
```sh
mvn clean package
smith
smith upload -r https://<user>:<pass>@registry-1.docker.io/fnproject/fn-java-fdk:jrebase
docker build . -t fnproject/fn-java-fdk
docker push fnproject/fn-java-fdk
```
