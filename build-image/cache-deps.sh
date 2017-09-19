#!/bin/bash

cd /tmp/cache-deps && mvn package dependency:copy-dependencies -Dmaven.repo.local=/usr/share/maven/ref/repository -DskipTests=true -Dmdep.prependGroupId=true -DoutputDirectory=target --fail-never
cd / && rm -fr /tmp/cache-deps
