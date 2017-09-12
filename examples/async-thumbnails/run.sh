#!/bin/bash

fn build

fn routes create myapp /async-thumbnails

STORAGE_SERVER_IP=`docker inspect --type container -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' example-storage-server`
fn routes config set myapp /async-thumbnails OBJECT_STORAGE_URL http://${STORAGE_SERVER_IP}:9000
fn routes config set myapp /async-thumbnails OBJECT_STORAGE_ACCESS alpha
fn routes config set myapp /async-thumbnails OBJECT_STORAGE_SECRET betabetabetabeta

curl -X POST --data-binary @test-image.png -H "Content-type: application/octet-stream" "http://localhost:8080/r/myapp/async-thumbnails"

echo "Contents of bucket"
mc ls -r example-storage-server
echo "Sleeping for 5 seconds to allow flows to complete"
sleep 5
echo "Contents of bucket"
mc ls -r example-storage-server
