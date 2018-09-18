# Java FDK integration tests 

Integration tests should only be used for: 

* Smoke testing of end-to-end features
* Rough validation of features that rely on interactions between the fn service and flow service 

They should _not_ be used for: 

* Feature testing of fn/fdk features that don't need cross-service features
* extensive feature testing (use unit tests)
* Performance/load testing 


# Running locally 


(in top-level dir)
```bash
export REPOSITORY_LOCATION=/tmp/staging-repository
# on OSX: 
export DOCKER_LOCALHOST=docker.for.mac.host.internal

mvn deploy -DaltDeploymentRepository=localStagingDir::default::file://"$REPOSITORY_LOCATION"
```

You may also want to/need build local copies of the build images: 
```bash 
cd build-image
./docker-build.sh -t fnproject/fn-java-fdk-build .
```

and runtime images: 
```
cd runtime
docker build -t fnproject/fn-java-fdk .
docker build -f Dockerfile-jdk9 -t fnproject/fn-java-fdk:jdk9-latest .
```

Finally you can run the integration tests: 

```bash
./integration-tests/run_tests_ci.sh
```


This will start/stop fnserver and flow server 