#!/bin/bash -ex

cd /tmp/staging-repository && python -mSimpleHTTPServer 18080 1>>/tmp/http-logs 2>&1 &
SRV_PROCESS=$!

docker build $*
kill $SRV_PROCESS
