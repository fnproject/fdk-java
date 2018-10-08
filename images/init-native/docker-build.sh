#!/bin/sh
if [ -z "${release_version}" ]
    then
    release_version=$(cat ../../release.version)
fi
BUILD_VERSION=${FN_FDK_VERSION:-1.0.0-SNAPSHOT}
sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${release_version}</fdk.version>|" pom.xml && rm pom.xml.bak
docker build -t fnproject/fn-java-native-init:${FN_FDK_VERSION} -f Dockerfile-init-image .
