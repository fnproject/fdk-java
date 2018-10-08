#!/bin/sh
if [ -z "${FN_FDK_VERSION}" ];  then
    FN_FDK_VERSION=$(cat ../../release.version)
fi
sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${FN_FDK_VERSION}</fdk.version>|" pom.xml && rm pom.xml.bak
docker build -t fnproject/fn-java-native-init:${FN_FDK_VERSION} -f Dockerfile-init-image .
