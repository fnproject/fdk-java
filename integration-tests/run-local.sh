#!/usr/bin/env bash

# Set up a local test environment in order to run integration tests,
# then execute them.

source "$(dirname "$0")/lib.sh"

set -ex

# ----------------------------------------------------------------------
# The following variables may be set to parameterise the operation of this script
# ----------------------------------------------------------------------

: ${FUNCTIONS_DOCKER_IMAGE:=fnproject/functions}
: ${SUFFIX:=$(git rev-parse HEAD)}
: ${COMPLETER_DOCKER_IMAGE:=fnproject/completer}

# ----------------------------------------------------------------------
# Check for prerequisites
# ----------------------------------------------------------------------

if [[ `echo nooo | sed -e "s/nooo/ok/"` != "ok" ]]; then
    echo "You need GNU sed installed, because this script uses 'sed -e'."
    echo "If you are on Mac, run 'brew install gnu-sed --with-default-names'."
    exit 2
fi

if [[ `echo grepme | grep -e "grepme"` != "grepme" ]]; then
    echo "You need GNU grep installed, because this script uses 'grep -e'."
    echo "If you are on Mac, run 'brew install gnu-grep --with-default-names'."
    exit 2
fi

# ----------------------------------------------------------------------
# Stand up a local staging maven directory, if needed
# ----------------------------------------------------------------------

if [[ -n "$REPOSITORY_LOCATION" ]]; then
    if [[ -n "$LOCALHOST_ACTUAL_IP" ]]; then
        echo "Using $LOCALHOST_ACTUAL_IP for the staging Maven repo."
        export MAVEN_REPOSITORY_LOCATION="http://$LOCALHOST_ACTUAL_IP:18080"
    else
        echo "No LOCALHOST_ACTUAL_IP set, assuming we're running on CircleCI."
        export MAVEN_REPOSITORY_LOCATION="http://172.17.0.1:18080"
    fi
    cd "$REPOSITORY_LOCATION" && python -mSimpleHTTPServer 18080 1>>/tmp/http-logs 2>&1 &
    defer kill -9 "$!"
fi

# ----------------------------------------------------------------------
# Stand up the functions platform
# ----------------------------------------------------------------------

docker pull $FUNCTIONS_DOCKER_IMAGE

FUNCTIONS_CONTAINER_ID=$(
    docker run -d \
        -p 8080 \
        -v /var/run/docker.sock:/var/run/docker.sock \
        --name functions-$SUFFIX \
        -e LOG_LEVEL=debug \
        $FUNCTIONS_DOCKER_IMAGE
    )
defer docker rm -f $FUNCTIONS_CONTAINER_ID
defer docker logs functions-$SUFFIX
defer echo ---- FUNCTIONS OUTPUT FOR TEST -----------------------------------------------------------

FUNCTIONS_HOST=$(
    docker inspect \
        --type container \
        -f '{{range index .NetworkSettings.Ports "8080/tcp"}}{{.HostIp}}{{end}}' \
        $FUNCTIONS_CONTAINER_ID
    )

FUNCTIONS_PORT=$(
    docker inspect \
        --type container \
        -f '{{range index .NetworkSettings.Ports "8080/tcp"}}{{.HostPort}}{{end}}' \
        $FUNCTIONS_CONTAINER_ID
    )

FUNCTIONS_INTERNAL_IP=$(
    docker inspect \
        --type container \
        -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'  \
        $FUNCTIONS_CONTAINER_ID
   )

export API_URL="http://$FUNCTIONS_HOST:$FUNCTIONS_PORT"
export no_proxy="$no_proxy,$FUNCTIONS_HOST"


# ----------------------------------------------------------------------
# Stand up the completer
# ----------------------------------------------------------------------

COMPLETER_CONTAINER_ID=$(
    docker run -d \
        -p 8081 \
        --env API_URL=http://${FUNCTIONS_INTERNAL_IP}:8080 \
        --env no_proxy=$no_proxy,${FUNCTIONS_INTERNAL_IP} \
        --name completer-$SUFFIX \
        $COMPLETER_DOCKER_IMAGE
    )
defer docker rm -f $COMPLETER_CONTAINER_ID
defer docker logs $COMPLETER_CONTAINER_ID
defer echo ---- COMPLETER OUTPUT FOR TEST -----------------------------------------------------------

COMPLETER_HOST=$(
    docker inspect \
        --type container \
        -f '{{range index .NetworkSettings.Ports "8081/tcp"}}{{.HostIp}}{{end}}' \
        $COMPLETER_CONTAINER_ID
    )

COMPLETER_PORT=$(
    docker inspect \
        --type container \
        -f '{{range index .NetworkSettings.Ports "8081/tcp"}}{{.HostPort}}{{end}}' \
        $COMPLETER_CONTAINER_ID
    )

COMPLETER_INTERNAL_IP=$(
    docker inspect \
        --type container \
        -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'  \
        $COMPLETER_CONTAINER_ID
   )

export COMPLETER_BASE_URL=http://$COMPLETER_INTERNAL_IP:8081
export no_proxy="$no_proxy,$COMPLETER_HOST"


# ----------------------------------------------------------------------
# Wait for the containers to become ready
# ----------------------------------------------------------------------

export HTTP_PROXY="$http_proxy"
export HTTPS_PROXY="$https_proxy"
export NO_PROXY="$no_proxy"

wait_for_http "$API_URL"
wait_for_http "http://$COMPLETER_HOST:$COMPLETER_PORT/ping"

set +x

"$SCRIPT_DIR/run-all-tests.sh" "$@"
