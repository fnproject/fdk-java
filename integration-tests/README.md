# Java FDK integration tests 

Integration tests should only be used for: 

* Smoke testing of end-to-end features
* Rough validation of features that rely on interactions between the fn service and flow service 

They should _not_ be used for: 

* Feature testing of fn/fdk features that don't need cross-service features
* extensive feature testing (use unit tests)
* Performance/load testing 

## Creating a new test 

Put main integration tests under main/test-<TestName> 

the content of a test dir is a a typically a new function (containing func.yaml, pom.xml etc. )

create the following files: 
* `input` : the input to pass to the deployed function 
* `expected` : the verbatim expected result of the function 
* `expected.sh` : a shell script that should succeed when the test passed  - this is used in place of `expected`
* `config` : A newline seperated list of config variables to set on the function 
* `pre-test.sh` a script that is run before the function is called (e.g. to call fn init to check bootstrapping)



# Running locally 

To run locally you will need to deploy the fn artifacts to a local repository: 

(in top-level dir)
```bash
export REPOSITORY_LOCATION=/tmp/staging-repository
# on OSX: 
export DOCKER_LOCALHOST=docker.for.mac.localhost 

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
./integration-tests/run-local.sh
```

Note that these will update the pom files in the tests - don't check these in! 


# Running against a remote environment
For running against a remote integration environment, configure
    ~/.fn-token
    ~/.fn-api-url
    ~/.fn-flow-base-url

and run the `run-remote.sh` script.
