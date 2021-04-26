#!/bin/bash
#
# this is instead for the maven release plugin (which sucks) - this sets the versions across the project before a build
# for branch builds these are ignored (nothing is deployed)
# For master releases this sets the latest version that this branch would be released as
#

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
   xargs -n 1 sed -i.bak -e "s|<fdk\\.version>.*</fdk\\.version>|<fdk.version>${release_version}</fdk.version>|"
find . -name pom.xml.bak -delete

