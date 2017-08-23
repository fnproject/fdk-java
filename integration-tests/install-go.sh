#!/usr/bin/env bash

source "$(dirname "$0")/lib.sh"

: ${GOVERSION:=1.8.3}
: ${OS:=linux}
: ${ARCH:=amd64}

set -ex

go version
go env GOROOT

pushd /tmp/go-$GOVERSION.$OS.$ARCH
    wget https://storage.googleapis.com/golang/go$GOVERSION.$OS-$ARCH.tar.gz
    sudo tar -C /usr/local -xzf go$GOVERSION.$OS-$ARCH.tar.gz
    go get -u github.com/golang/dep/...
popd

go version
go env GOROOT
