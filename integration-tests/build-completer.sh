#!/usr/bin/env bash

source "$(dirname "$0")/lib.sh"
set -e

COMPLETER_PATH=/tmp/completer

if [ ! -d /tmp/completer ]; then
    git clone git@github.com:fnproject/completer.git /tmp/completer
else
    pushd /tmp/completer
        git reset --hard
        git clean -df
        git pull
        git checkout master
    popd
fi

pushd /tmp/completer
    make docker-build
popd

