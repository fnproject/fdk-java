#!/bin/bash

# Run all smoke-tests in parallel, recording their output.
# Report the results on any failures.

source "$SCRIPT_DIR/lib.sh"

set -ex

# ----------------------------------------------------------------------
# The following variables need to be set
# ----------------------------------------------------------------------

# This is an awful bashism
if [[ -z "${FN_API_URL+x}" ]]; then echo "Please set FN_API_URL"; exit 1; fi
if [[ -z "${COMPLETER_BASE_URL+x}" ]]; then echo "Please set COMPLETER_BASE_URL"; exit 1; fi


# ----------------------------------------------------------------------
# Run each smoke-test in parallel
# ----------------------------------------------------------------------

printenv
fn apps list

set +x

SMOKE_HARNESS="$SCRIPT_DIR/smoke-test.sh"
export LIBFUNS="$SCRIPT_DIR/lib.sh"
export FN_TOKEN
export no_proxy=$no_proxy,127.0.0.1,10.167.103.241

cd "$SCRIPT_DIR"

if [[ $# = 0 ]]; then
  tests=main/test-*
  show=
  background='> "$d/output" 2>&1 &'
else
  tests=$(find "$@" -type d -name test-\* -prune)
  show='set -x'
  background=
fi

echo "Running tests: $tests"

eval "$show"
for d in $tests
do

  rm -f "$d"/actual "$d"/output
  eval "(
    # Run the integration test

    cd \"$d\" && \"$SMOKE_HARNESS\"
  ) $background"

done
wait
set +x


# ----------------------------------------------------------------------
# Report on results sequentially
# ----------------------------------------------------------------------

okay=1
report() {
    echo "Test $(basename "$1") expected -"
    cat "$1/expected"
    echo "Test $(basename "$1") actual -"
    cat "$1/actual"
    echo "Test $(basename "$1") output -"
    cat "$1/output"
    line
}

for d in $tests
do
  set +e

  if [[ -f "$d/failure" ]]; then
    okay=0
    line
    echo "Test $(basename "$d") failed:"
    report "$d"
  elif [[ -f "$d/success" ]]; then
    line
    echo "Test $(basename "$d") succeeded"
    line
  else
    okay=0
    line
    echo "**************** Test $(basename "$d") unknown status"
    report "$d"
  fi
done

[[ $okay = 1 ]] || exit 1
echo Success!
