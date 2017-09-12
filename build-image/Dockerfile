FROM maven:3-jdk-8-alpine

ADD pom.xml /function/pom.xml

WORKDIR /function
RUN ["mvn", "package", "dependency:copy-dependencies", "-Dmaven.repo.local=/usr/share/maven/ref/repository", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]
RUN ["rm", "/function/pom.xml"]
WORKDIR /
