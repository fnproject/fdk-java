# Java FDK integration tests 

Integration tests should only be used for: 

* Smoke testing of end-to-end flows
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
* `expected.sh` : a shell script that should succeed when the test passed 
* `config` : A newline seperated list of config variables to set on the function 
* `pre-test.sh` a script that is run before the function is called (e.g. to call fn init to check bootstrapping)


Put functions platform behaviour tests under functions/test-*

Categories of tests can be run locally with `run-local-integration-tests main` to
select a subset of tests; these are then run serially.

For running against a remote integration environment, configure
    ~/.fn-token
    ~/.fn-api-url
    ~/.fn-flow-base-url
and run the `run-remote.sh` script.
