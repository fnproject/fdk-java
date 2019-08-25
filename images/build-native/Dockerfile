FROM openjdk:8-jdk-slim as build
LABEL maintainer="tomas.zezula@oracle.com"

RUN set -x \
    && apt-get -y update \
    && apt-get -y install gcc g++ git make python zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

ENV JVMCI_VERSION 19.2-b02

WORKDIR /build

RUN set -x \
    && git clone https://github.com/graalvm/mx.git \
    && git clone https://github.com/graalvm/graal-jvmci-8.git \
    && git -C graal-jvmci-8 checkout jvmci-${JVMCI_VERSION} \
    && mx/mx --primary-suite graal-jvmci-8 --vm=server build -DFULL_DEBUG_SYMBOLS=0 \
    && mx/mx --primary-suite graal-jvmci-8 --vm=server -v vm -version \
    && mx/mx --primary-suite graal-jvmci-8 --vm=server -v unittest \
    && cp -r $(/build/mx/mx --primary-suite graal-jvmci-8 jdkhome) /build/jvmcijdk8

RUN git clone https://github.com/oracle/graal.git \
    && git -C graal checkout vm-19.2.0
WORKDIR /build/graal/vm
RUN export JAVA_HOME=/build/jvmcijdk8 \
    && /build/mx/mx --dy /substratevm --force-bash-launchers=true --disable-polyglot --disable-libpolyglot build

WORKDIR /build/graal/vm/latest_graalvm
RUN LONG_NAME=$(ls) \
    && SHORT_NAME=graalvm \
    && mv $LONG_NAME $SHORT_NAME

FROM debian:stretch-slim as final
LABEL maintainer="tomas.zezula@oracle.com"

RUN set -x \
    && apt-get -y update \
    && apt-get -y install gcc zlib1g-dev

COPY --from=build /build/graal/vm/latest_graalvm/graalvm /usr/local/graalvm
COPY src/main/c/libfnunixsocket.so /function/runtime/lib/


ENV GRAALVM_HOME=/usr/local/graalvm
WORKDIR /function
