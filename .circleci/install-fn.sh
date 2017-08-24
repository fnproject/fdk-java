#!/usr/bin/env bash

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
    go build -o fn
    sudo cp fn "$FN_INSTALL_PATH"
popd
