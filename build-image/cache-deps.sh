#!/bin/bash

cd /tmp/cache-deps && mvn test package dependency:copy-dependencies -DproxyHost=www-proxy.uk.oracle.com -DproxyPort=80 -Dmaven.repo.local=/usr/share/maven/ref/repository -Dmdep.prependGroupId=true -DoutputDirectory=target --fail-never
cd / && rm -fr /tmp/cache-deps
