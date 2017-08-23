#!/usr/bin/env bash

source "$(dirname "$0")/lib.sh"

COMPLETER_PATH=/tmp/completer

if [ ! -d /tmp/completer ]; then
    git clone git@github.com:fnproject/completer.git /tmp/completer || error "Cannot pull completer git repo"
else
    pushd /tmp/completer
        git reset --hard
        git clean -df
        git pull
        git checkout master
    popd
fi

pushd /tmp/completer
    make docker-build || error "Cannot build completer"
popd

