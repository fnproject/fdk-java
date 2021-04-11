#!/bin/bash

set -e
USER=fnproject

SERVICE=fn-java-fdk
RUNTIME_IMAGE=${SERVICE}
BUILD_IMAGE=${SERVICE}-build
NATIVE_BUILD_IMAGE=fn-java-native
NATIVE_INIT_IMAGE=${NATIVE_BUILD_IMAGE}-init

release_version=$(cat release.version)
if [[ ${release_version} =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
   echo "Deploying version $release_version"
else
   echo Invalid version $release_version
   exit 1
fi

graalvm_version=$(cat graalvm.version)

# Calculate new version
version_parts=(${release_version//./ })
new_minor=$((${version_parts[2]}+1))
new_version="${version_parts[0]}.${version_parts[1]}.$new_minor"

if [[ ${new_version} =~  ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
   echo "Next version $new_version"
else
   echo Invalid new version ${new_version}
   exit 1
fi

# Push result to git

echo ${new_version} > release.version
git tag -a "$release_version" -m "version $release_version"
git add release.version
git commit -m "$SERVICE: post-$release_version version bump [skip ci]"
git push
git push origin "$release_version"


# Regenerate runtime and build images and push them
(
  moving_version=${release_version%.*}-latest

  ## jdk8 runtime
  docker tag ${USER}/${RUNTIME_IMAGE}:${release_version} ${USER}/${RUNTIME_IMAGE}:latest
  docker tag ${USER}/${RUNTIME_IMAGE}:${release_version} ${USER}/${RUNTIME_IMAGE}:${moving_version}
  docker push ${USER}/${RUNTIME_IMAGE}:latest
  docker push ${USER}/${RUNTIME_IMAGE}:${release_version}
  docker push ${USER}/${RUNTIME_IMAGE}:${moving_version}

  ## jdk8 build
  docker tag ${USER}/${BUILD_IMAGE}:${release_version} ${USER}/${BUILD_IMAGE}:latest
  docker tag ${USER}/${BUILD_IMAGE}:${release_version} ${USER}/${BUILD_IMAGE}:${moving_version}
  docker push ${USER}/${BUILD_IMAGE}:latest
  docker push ${USER}/${BUILD_IMAGE}:${release_version}
  docker push ${USER}/${BUILD_IMAGE}:${moving_version}

  ## jre11 runtime
  docker tag ${USER}/${RUNTIME_IMAGE}:jre11-${release_version} ${USER}/${RUNTIME_IMAGE}:jre11-latest
  docker tag ${USER}/${RUNTIME_IMAGE}:jre11-${release_version} ${USER}/${RUNTIME_IMAGE}:jre11-${moving_version}
  docker push ${USER}/${RUNTIME_IMAGE}:jre11-latest
  docker push ${USER}/${RUNTIME_IMAGE}:jre11-${release_version}
  docker push ${USER}/${RUNTIME_IMAGE}:jre11-${moving_version}

  ## jdk11 build
  docker tag ${USER}/${BUILD_IMAGE}:jdk11-${release_version} ${USER}/${BUILD_IMAGE}:jdk11-latest
  docker tag ${USER}/${BUILD_IMAGE}:jdk11-${release_version} ${USER}/${BUILD_IMAGE}:jdk11-${moving_version}
  docker push ${USER}/${BUILD_IMAGE}:jdk11-latest
  docker push ${USER}/${BUILD_IMAGE}:jdk11-${release_version}
  docker push ${USER}/${BUILD_IMAGE}:jdk11-${moving_version}

  ## native jdk8 build image
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:latest
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:${moving_version}
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:${release_version}-graalvm-ce-${graalvm_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:latest
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:${release_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:${moving_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:${release_version}-graalvm-ce-${graalvm_version}

  ## native jdk11 build image
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-latest
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${moving_version}
  docker tag ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version}-graalvm-ce-${graalvm_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-latest
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${moving_version}
  docker push ${USER}/${NATIVE_BUILD_IMAGE}:jdk11-${release_version}-graalvm-ce-${graalvm_version}

  ## jdk8 native init image
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:${release_version} ${USER}/${NATIVE_INIT_IMAGE}:latest
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:${release_version} ${USER}/${NATIVE_INIT_IMAGE}:${moving_version}
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:${release_version} ${USER}/${NATIVE_INIT_IMAGE}:${release_version}-graalvm-ce-${graalvm_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:latest
  docker push ${USER}/${NATIVE_INIT_IMAGE}:${release_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:${moving_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:${release_version}-graalvm-ce-${graalvm_version}

  ## jdk11 native init image
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_INIT_IMAGE}:jdk11-latest
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${moving_version}
  docker tag ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version} ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version}-graalvm-ce-${graalvm_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:jdk11-latest
  docker push ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${moving_version}
  docker push ${USER}/${NATIVE_INIT_IMAGE}:jdk11-${release_version}-graalvm-ce-${graalvm_version}   
)


# Deploy to Maven Central OSSRH
mvn -s ./settings-deploy.xml \
    -DskipTests \
    clean deploy \
    -Pci-cd
