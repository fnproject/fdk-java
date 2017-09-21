#!/bin/bash

cd /tmp/cache-deps && mvn test package dependency:copy-dependencies -Dmaven.repo.local=/usr/share/maven/ref/repository -Dmdep.prependGroupId=true -DoutputDirectory=target
cd / && rm -fr /tmp/cache-deps
