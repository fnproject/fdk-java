#!/bin/bash

release_version=$(cat release.version)
if [[ $release_version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
   echo "Deploying version $release_version"
else
   echo Invalid version $release_version
   exit 1
fi

mvn versions:set -D newVersion=${release_version} versions:update-child-modules


# We need to replace the example dependency versions also
# (sed syntax for portability between MacOS and gnu)
find . -name pom.xml |
   xargs -n 1 sed -i.bak -e "s|<fnproject\\.version>.*</fnproject\\.version>|<fnproject.version>${release_version}</fnproject.version>|"
find . -name pom.xml.bak -delete

