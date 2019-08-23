FROM maven:3-jdk-8-slim@f7ffbcfeae6e5d56019afeb8c4b288afe73d25b4a0b0b2954fe5866143c1dda0
ARG FN_REPO_URL
ADD pom.xml /tmp/cache-deps/pom.xml
ADD cache-deps.sh /tmp/cache-deps/cache-deps.sh
ADD src /tmp/cache-deps/src

RUN /tmp/cache-deps/cache-deps.sh
