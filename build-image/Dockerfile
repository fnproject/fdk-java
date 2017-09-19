FROM maven:3-jdk-8-alpine

ADD pom.xml /tmp/cache-deps/pom.xml
ADD cache-deps.sh /tmp/cache-deps/cache-deps.sh
ADD src /tmp/cache-deps/src

RUN /tmp/cache-deps/cache-deps.sh
