#!/bin/bash -ex

cd /tmp/cache-deps && mvn test package dependency:copy-dependencies -Dmaven.repo.local=/usr/share/maven/ref/repository -Dmdep.prependGroupId=true -DoutputDirectory=target --settings /tmp/settings.xml
cd / && rm -fr /tmp/cache-deps
