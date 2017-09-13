#!/bin/bash

set -ex
set -x
USER=fnproject
SERVICE=fn-java-fdk

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

// Calculate new version
version_parts=(${release_version//./ })
new_minor=$((${version_parts[2]}+1))
new_version="${version_parts[0]}.${version_parts[1]}.$new_minor"

if [[ $new_version =~  ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
   echo "Next version $new_version"
else
   echo Invalid new version $new_version
   exit 1
fi


# Deploy to bintray
echo mvn -s ./settings-deploy.xml \
    -DskipTests \
    -DaltDeploymentRepository="fnproject-release-repo::default::$MVN_RELEASE_REPO" \
    -Dfnproject-release-repo.username="$MVN_RELEASE_USER" \
    -Dfnproject-release-repo.password="$MVN_RELEASE_PASSWORD" \
    -DdeployAtEnd=true \
    clean deploy

# Regenerate runtime image and push it
(
  moving_version=${release_version%.*}-latest

  docker tag $USER/$SERVICE:latest $USER/$SERVICE:${release_version}
  docker tag $USER/$SERVICE:latest $USER/$SERVICE:${moving_version}
  docker push $USER/$SERVICE:latest
  docker push $USER/$SERVICE:${release_version}
  docker push $USER/$SERVICE:${moving_version}
)



# Push result to git

echo $next_version > release.version
git tag -a "$release_version" -m "version $release_version"
git add release.version
git commit -m "$SERVICE: post-$release_version version bump [skip ci]"
git push
git push origin "$release_version"
