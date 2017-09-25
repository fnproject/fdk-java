#!/bin/bash -ex

. "$(dirname "$0")"/../integration-tests/lib.sh

CID=$(docker run \
          -d \
          -v /tmp/staging-repository:/tmp/staging-repository \
          -p 18080 \
          python:2.7 \
          /bin/sh -c '
    set -e
    cd /tmp/staging-repository
    python -mSimpleHTTPServer 18080')

defer docker rm -f "$CID"

IP=$(docker inspect --type container "$CID" -f '{{ .NetworkSettings.IPAddress }}')

T=$(mktemp -d)
defer rm -rf "$T"

(
cp -R . "$T/"

cd "$T"
sed -i.bak -e "s/%LOCAL_SERVER%/$IP/" pom.xml

# Fix up a settings.xml if required
if [ -z "$http_proxy" ]; then
  PROXY_ACTIVE=false
  PROXY_PROTO=
  PROXY_HOST=
  PROXY_PORT=
  PROXY_NOPROXY=
else
  PROXY_ACTIVE=true
  PROXY_PROTO=${http_proxy%%:*}
  PROXY_HOST=$(echo "$http_proxy" | sed -e 's|.*://||' -e 's|:.*||')
  PROXY_PORT=$(echo "$http_proxy" | sed -e 's|.*://.*:||' -e 's|/$||')
  PROXY_NOPROXY=$(echo "$no_proxy" |
      tr ',' '\n' |
      sed -e 's|^\.|*.|' -e 's|\.$|.*|' |
      tr '\n' '|')
fi

sed -i.bak \
    -e "s/%PROXY_ACTIVE%/$PROXY_ACTIVE/g" \
    -e "s/%PROXY_PROTO%/$PROXY_PROTO/g" \
    -e "s/%PROXY_HOST%/$PROXY_HOST/g" \
    -e "s/%PROXY_PORT%/$PROXY_PORT/g" \
    -e "s/%PROXY_NOPROXY%/$PROXY_NOPROXY/g" \
    settings.xml

docker build "$@"
)
