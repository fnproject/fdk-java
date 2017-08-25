#!/usr/bin/env bash

: ${GOVERSION:=1.8.3}
: ${OS:=linux}
: ${ARCH:=amd64}

set -ex

go version
go env GOROOT

# Remove previous Go version
sudo rm -rf /usr/local/go

# Install Go
BUILD_DIR="/tmp/go-$GOVERSION.$OS.$ARCH"
mkdir -p "$BUILD_DIR"
pushd "$BUILD_DIR"
    wget https://storage.googleapis.com/golang/go$GOVERSION.$OS-$ARCH.tar.gz
    sudo tar -C /usr/local -xzf go$GOVERSION.$OS-$ARCH.tar.gz
    go get -u github.com/golang/dep/...
popd

# Install Glide
if ! type glide > /dev/null; then
    curl https://glide.sh/get | sh
fi

go version
go env GOROOT
