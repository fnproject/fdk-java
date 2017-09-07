FROM maven:3-jdk-8-alpine

ADD pom.xml /function/pom.xml

WORKDIR /function
RUN ["mvn", "package", "dependency:copy-dependencies", "-Dhttp.proxyHost=www-proxy.uk.oracle.com", "-Dhttps.proxyHost=www-proxy.uk.oracle.com", "-Dhttp.proxyPort=80", "-Dhttps.proxyPort=80", "-Dmaven.repo.local=/usr/share/maven/ref/repository", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]
RUN ["rm", "/function/pom.xml"]
WORKDIR /
