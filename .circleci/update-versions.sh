#!/bin/bash

# Revs the version on master
#if [ "${CIRCLE_BRANCH}" != "master" ]; then
#   echo Trying to rev versions on non-master branch
#   exit 1
#fi

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
find examples -name pom.xml.bak -delete

