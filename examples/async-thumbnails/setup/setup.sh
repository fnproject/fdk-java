#!/bin/bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Checks
if docker ps >/dev/null ; then
    echo "Docker is present."
else
    echo "error: docker is not available."
    exit 1
fi
if fn --help >/dev/null ; then
    echo "Fn is present."
else
    echo "error: fn is not available."
    exit 1
fi

# Start functions server if not there
if [[ -z `docker ps | grep "functions"` ]]; then
    docker run -d --name functions -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock fnproject/functions:latest
    # Give it time to start up
    sleep 3
else
    echo "Functions server is already up."
fi
# Get its IP
FUNCTIONS_SERVER_IP=`docker inspect --type container -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' functions`

# Start cloud-completer if not there
if [[ -z `docker ps | grep "cloud-completer"` ]]; then
    docker run -d --name cloud-completer -p 8081:8081 --env COMPLETER_HOST=0.0.0.0 --env COMPLETER_PORT=8081 --env FN_HOST=$FUNCTIONS_SERVER_IP --env FN_PORT=8080 cloud-completer
    # Give it time to start up
    sleep 3
else
    echo "Cloud Completer server is already up."
fi
# Get its IP
COMPLETER_SERVER_IP=`docker inspect --type container -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' cloud-completer`

# Create app and routes
if [[ `fn apps list` == *"myapp"* ]]; then
    echo "App myapp is already there."
else
    fn apps create myapp
    fn apps config set myapp COMPLETER_BASE_URL http://${COMPLETER_SERVER_IP}:8081
fi

if [[ `fn routes list myapp` == *"/resize128"* ]]; then
    echo "Route /resize128 is already there."
else
    # This works around proxy issues
    cd $SCRIPT_DIR/resize128 && \
        docker build -t example/resize128:0.0.1 --build-arg http_proxy=$http_proxy --build-arg https_proxy=$https_proxy . && \
        fn routes create myapp /resize128
fi
if [[ `fn routes list myapp` == *"/resize256"* ]]; then
    echo "Route /resize256 is already there."
else
    # This works around proxy issues
    cd $SCRIPT_DIR/resize256 && \
        docker build -t example/resize256:0.0.1 --build-arg http_proxy=$http_proxy --build-arg https_proxy=$https_proxy . && \
        fn routes create myapp /resize256
fi
if [[ `fn routes list myapp` == *"/resize512"* ]]; then
    echo "Route /resize512 is already there."
else
    # This works around proxy issues
    cd $SCRIPT_DIR/resize512 && \
        docker build -t example/resize512:0.0.1 --build-arg http_proxy=$http_proxy --build-arg https_proxy=$https_proxy . && \
        fn routes create myapp /resize512
fi

# Set up the storage server and upload directory
if [[ -z `docker ps | grep "example-storage-server"` ]]; then
    mkdir -p $SCRIPT_DIR/../storage-upload
    docker run -d -e "MINIO_ACCESS_KEY=alpha" -e "MINIO_SECRET_KEY=betabetabetabeta" -v $SCRIPT_DIR/../storage-upload:/export --name example-storage-server -p 9000:9000 minio/minio server /export
    # Give it time to start up
    sleep 3
else
    echo "Storage server is already up."
fi
