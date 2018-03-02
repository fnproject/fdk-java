#!/usr/bin/env bash

# Set up a local test environment in order to run integration tests,
# then execute them.

source "$(dirname "$0")/lib.sh"

set -ex

# ----------------------------------------------------------------------
# The following variables may be set to parameterise the operation of this script
# ----------------------------------------------------------------------

: ${FUNCTIONS_DOCKER_IMAGE:=fnproject/fnserver}
: ${SUFFIX:=$(git rev-parse HEAD)}
: ${COMPLETER_DOCKER_IMAGE:=fnproject/flow}

# ----------------------------------------------------------------------
# Stand up a local staging maven directory, if needed
# ----------------------------------------------------------------------

if [[ -n "$REPOSITORY_LOCATION" ]]; then
    REPO_CONTAINER_ID=$(
        docker run -d \
            -v "$REPOSITORY_LOCATION":/repo:ro \
            -w /repo \
            --name repo-$SUFFIX \
            python:2.7 \
            python -mSimpleHTTPServer 18080
    )
    defer docker rm -f $REPO_CONTAINER_ID
    REPO_INTERNAL_IP=$(
        docker inspect \
            --type container \
            -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'  \
            $REPO_CONTAINER_ID
       )
    export MAVEN_REPOSITORY_LOCATION="http://$REPO_INTERNAL_IP:18080"
    export no_proxy="$no_proxy,$REPO_INTERNAL_IP"
fi

# ----------------------------------------------------------------------
# Stand up the functions platform
# ----------------------------------------------------------------------

docker pull $FUNCTIONS_DOCKER_IMAGE

FUNCTIONS_CONTAINER_ID=$(
    docker run -d \
        -p 8080:8080 \
        -v /var/run/docker.sock:/var/run/docker.sock \
        --name functions-$SUFFIX \
        -e FN_LOG_LEVEL=debug \
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

export FN_API_URL="http://$FUNCTIONS_HOST:$FUNCTIONS_PORT"
export no_proxy="$no_proxy,$FUNCTIONS_HOST"


# ----------------------------------------------------------------------
# Stand up the completer
# ----------------------------------------------------------------------

COMPLETER_CONTAINER_ID=$(
    docker run -d \
        -p 8081 \
        --env API_URL=http://${FUNCTIONS_INTERNAL_IP}:8080 \
        --env no_proxy=$no_proxy,${FUNCTIONS_INTERNAL_IP} \
        --name flow-server-$SUFFIX \
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

wait_for_http "$FN_API_URL"
wait_for_http "http://$COMPLETER_HOST:$COMPLETER_PORT/ping"

set +x

"$SCRIPT_DIR/run-all-tests.sh" "$@"
