Put main integration tests under main/test-*

Put functions platform behaviour tests under functions/test-*

Categories of tests can be run locally with `run-local-integration-tests main` to
select a subset of tests; these are then run serially.

For running against a remote integration environment, configure
    ~/.fn-token
    ~/.fn-api-url
    ~/.fn-flow-base-url
and run the `run-remote.sh` script.
