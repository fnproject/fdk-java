# Using this to test

You should be able to see a trace of the various calls
in the function response.

## To build the dependencies:

- package up the api and runtime

- build a docker image of the runtime

## To test:

    fn build
    fn call --display-call-id myapp /test

or:

    echo 1, 3 | fn call --display-call-id myapp /test
