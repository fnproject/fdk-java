#!/bin/bash -ex

set -x
set -e

if [ -n "$FN_REPO_URL" ]; then
  REPO_DFN="-Dfn.repo.url=$FN_REPO_URL"
fi


cd /tmp/cache-deps && mvn test package dependency:copy-dependencies -Dmaven.repo.local=/usr/share/maven/ref/repository -Dmdep.prependGroupId=true -DoutputDirectory=target $REPO_DFN
cd / && rm -fr /tmp/cache-deps
