#!/bin/bash

. "$LIBFUNS"

# Run an individual smoketest

# Environmental requirements:
# - cwd is the directory of the smoketest to run
# - LIBFUNS points to the shell helper library
# - up-to-date "fn" command on the PATH
# - FN_TOKEN is set to something that the functions platform will approve
# - FN_API_URL points to the functions platform endpoint
# - any http_proxy, etc. settings are correct to permit access to that endpoint and any maven repos required by fn build
# - COMPLETER_BASE_URL is set to a value that should be configured on the target function
# - MAVEN_REPOSITORY_LOCATION, if set, corresponds to the URL that should be replaced in the test pom files.
# - the runtime docker image is up-to-date

rm -f success failure Dockerfile
export TESTNAME="$(basename $(pwd))"
set -ex

if [ -f pre-test.sh ]; then
    ./pre-test.sh
fi

# Replace the maven repo with a staging location, if required
if [ -n "$MAVEN_REPOSITORY_LOCATION" ]; then
    sed -i.bak \
        -e "s|https://dl.bintray.com/fnproject/fnproject|$MAVEN_REPOSITORY_LOCATION|g" \
        pom.xml
    rm pom.xml.bak
fi

# Build the integration test

[[ -n "$PRE_BUILD_HOOK" ]] && $PRE_BUILD_HOOK

fn build --no-cache >build-output 2>&1 || {
    echo "Test function build failed:"
    cat build-output
    exit 1
}

if [ -f config ]; then
    fn apps create "$TESTNAME" $(echo $(prefix_lines --config config))
else
    fn apps create "$TESTNAME"
fi

if [[ -x deploy.sh ]]
then
    ./deploy.sh
else
     fn deploy --app "$TESTNAME" --local
fi

[[ -n "$POST_CONFIGURE_HOOK" ]] && $POST_CONFIGURE_HOOK

fn apps inspect "$TESTNAME"
[[ -x route-create.sh ]] || fn routes inspect "$TESTNAME" "$TESTNAME"

if [[ -x run-test.sh ]]
then
    ./run-test.sh
else
    curl -v "$FN_API_URL/r/$TESTNAME/$TESTNAME" -d @input > actual
fi

if [[ -x expected.sh ]]
then
    ./expected.sh && touch success || touch failure
else
    diff --ignore-all-space -u expected actual && touch success || touch failure
fi

set +x
fn calls list "$TESTNAME" | while read k v
do
  echo "$k $v"
  if [[ "$k" = "ID:" ]]; then id="$v"; fi
  if [[ -z "$k" ]]; then
    echo '[[['
    fn logs get "$TESTNAME" "$id"
    echo ']]]'
    echo
  fi
done

set -x

if [[ -x delete.sh ]]
then
    ./delete.sh
fi
fn apps delete "$TESTNAME"
