FROM openjdk:8-jre-slim@8489e5a8e8a144ae7c41cbc2b95de8a7618cc31c7ae3ecb9db8d4b667ee84ff1 as build
LABEL maintainer="tomas.zezula@oracle.com"

RUN set -x \
    && mkdir /usr/share/man/man1 \
    && apt-get -y update \
    && apt-get -y install gcc g++ git make openjdk-8-doc openjdk-8-source python zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

ENV JVMCI_VERSION 19-b04

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
    && git -C graal checkout vm-19.0.2
WORKDIR /build/graal/vm
RUN export JAVA_HOME=/build/jvmcijdk8 \
    && /build/mx/mx --dy /substratevm --force-bash-launchers=true --disable-polyglot --disable-libpolyglot build

WORKDIR /build/graal/vm/latest_graalvm
RUN LONG_NAME=$(ls) \
    && SHORT_NAME=graalvm \
    && mv $LONG_NAME $SHORT_NAME

FROM debian:stretch-slim@21bdee09aac385973b3568feaf91c12bac8a9852caa04067ba3707dcd68b70e6 as final
LABEL maintainer="tomas.zezula@oracle.com"

RUN set -x \
    && apt-get -y update \
    && apt-get -y install gcc zlib1g-dev

COPY --from=build /build/graal/vm/latest_graalvm/graalvm /usr/local/graalvm
COPY src/main/c/libfnunixsocket.so /function/runtime/lib/


ENV GRAALVM_HOME=/usr/local/graalvm
WORKDIR /function
