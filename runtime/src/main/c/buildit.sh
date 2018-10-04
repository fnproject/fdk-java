#!/usr/bin/env bash
set -e

src_dir=$(pwd)
build_dir=${src_dir}/build/$(uname -s| tr '[:upper:]' '[:lower:]')

mkdir -p ${build_dir}
(
    cd  ${build_dir}
    cmake ${src_dir}

    make
)
mv ${build_dir}/libfnunixsocket.* ${src_dir}