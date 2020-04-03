#!/bin/sh
if [ -z "${FN_FDK_VERSION}" ];  then
    FN_FDK_VERSION=$(cat ../../release.version)
fi
sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${FN_FDK_VERSION}</fdk.version>|" pom.xml && rm pom.xml.bak
cp Dockerfile Dockerfile.build
docker build -t fnproject/fn-java-native-init:${FN_FDK_VERSION} -f Dockerfile-init-image .

cp Dockerfile Dockerfile.build
sed -i.bak -e "s|fnproject/fn-java-fdk-build:latest|fnproject/fn-java-fdk-build:jdk11-latest|" Dockerfile.build && rm Dockerfile.build.bak
sed -i.bak -e "s|fnproject/fn-java-native:latest|fnproject/fn-java-native:jdk11-latest|" Dockerfile.build && rm Dockerfile.build.bak
docker build -t fnproject/fn-java-native-init:jdk11-${FN_FDK_VERSION} -f Dockerfile-init-image .
rm Dockerfile.build