FROM fnproject/fn-java-fdk-build:__TO_BE_REPLACED__ as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository
ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]
ADD src /function/src
RUN ["mvn", "package"]
FROM fnproject/fn-java-fdk:__TO_BE_REPLACED__
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/
CMD ["com.fnproject.fn.integration.test_1.CompleterFunction::handleRequest"]
