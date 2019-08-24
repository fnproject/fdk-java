# Java FDK integration tests 

Integration tests should only be used for: 

* Smoke testing of end-to-end features
* Rough validation of features that rely on interactions between the fn service and flow service 

They should _not_ be used for: 

* Feature testing of fn/fdk features that don't need cross-service features
* extensive feature testing (use unit tests)
* Performance/load testing 


# Running locally 

Build the runtime: 
```bash
./build.sh 
```

Run the integration tests: 

```bash
./integration-tests/run_tests_ci.sh
```


This will start/stop fnserver and flow server 