#!/usr/bin/env bash

# Set up a local test environment in order to run integration tests,
# then execute them.

source "$(dirname "$0")/lib.sh"

set -ex

export GOPATH=/tmp/go
FN_SRC_DIR="$GOPATH/src/github.com/fnproject/fn"
FN_INSTALL_PATH=/usr/local/bin/fn

mkdir -p "$GOPATH"

if [ ! -d "$FN_SRC_DIR" ]; then
    git clone git@github.com:fnproject/fn.git "$FN_SRC_DIR"
else
    pushd "$FN_SRC_DIR"
        git reset --hard
        git clean -df
        git checkout master
        git pull
    popd
fi

pushd "${FN_SRC_DIR}/cli"
    make build
    if [ -f fn ]; then
        cp -a fn "$FN_INSTALL_PATH"
    else
        error "fn binary not built, so can't install"
    fi
popd
