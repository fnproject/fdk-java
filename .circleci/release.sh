#!/bin/bash

set -ex

USER=fnproject
SERVICE=fn-java-fdk

# Get current version
current_snapshot_version=$(mvn help:evaluate -Dexpression=project.version | grep -v '^\[INFO\]')  # sigh

# Confirm that it's a -SNAPSHOT or bail out
case "$current_snapshot_version" in
  *.*.*-SNAPSHOT) ;;
  *)
     echo "Error: expected *-SNAPSHOT project version, found $current_snapshot_version"
     exit 1
     ;;
esac

# Replace with a release version
release_version=${current_snapshot_version%-SNAPSHOT}
mvn versions:set -D newVersion=${release_version} versions:update-child-modules

# We need to replace the example dependency versions also
# (sed syntax for portability between MacOS and gnu)
find examples -name pom.xml |
    xargs -n 1 sed -i.bak -e "s|<sdk\\.version>.*</sdk\\.version>|<sdk.version>${release_version}</sdk.version>|"
find examples -name pom.xml.bak -delete

# Deploy to bintray
mvn -s ./settings-deploy.xml \
    -DskipTests \
    -DaltDeploymentRepository="fnproject-release-repo::default::$MVN_RELEASE_REPO" \
    -Dfnproject-release-repo.username="$MVN_RELEASE_USER" \
    -Dfnproject-release-repo.password="$MVN_RELEASE_PASSWORD" \
    -DdeployAtEnd=true \
    clean deploy

# Regenerate runtime image and push it
(
  cd runtime
  docker build -t $USER/$SERVICE:${release_version} .
  moving_version=${release_version%.*}-latest
  docker tag $USER/$SERVICE:${release_version} $USER/$SERVICE:${moving_version}

  docker push $USER/$SERVICE:${release_version}
  docker push $USER/$SERVICE:${moving_version}
)

# Bump snapshot version

mvn versions:revert
mvn versions:set -D nextSnapshot=true
mvn versions:commit

# Get next snapshot version and replace that in the integration tests
new_snapshot_version=$(mvn help:evaluate -Dexpression=project.version | grep -v '^\[INFO\]')  # sigh, again

find integration-tests/main -name pom.xml |
    xargs -n 1 sed -i.bak -e "s|<fnproject\\.version>.*</fnproject\\.version>|<fnproject.version>${new_snapshot_version}</fnproject.version>|"

find examples -name pom.xml |
    xargs -n 1 sed -i.bak -e "s|<sdk\\.version>.*</sdk\\.version>|<sdk.version>${new_snapshot_version}</sdk.version>|"

find integration-tests/main examples -name pom.xml.bak -delete

# The examples are not updated automatically. Do all of those.
for pom in examples/*/pom.xml; do
  (
    cd "$(dirname "$pom")"
    mvn versions:set -D newVersion=${new_snapshot_version}
    mvn versions:commit
  )
done

# Push result to git

git tag -a "$release_version" -m "version $release_version"
git commit -a -m "$SERVICE: post-$release_version version bump [skip ci]"
git push
git push origin "$release_version"
