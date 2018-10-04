FROM alpine:latest
LABEL maintainer="tomas.zezula@oracle.com"

WORKDIR /build
COPY src src
COPY pom.xml .
COPY func.init.yaml .
COPY Dockerfile .

RUN echo $'#!/bin/sh\n\
if [ -n ${FN_FUNCTION_NAME} ]\n\
    then\n\
    JAVA_NAME=$(echo ${FN_FUNCTION_NAME:0:1} | tr "[:lower:]" "[:upper:]")${FN_FUNCTION_NAME:1}\n\
    sed -i -e "s|<artifactId>hello</artifactId>|<artifactId>${FN_FUNCTION_NAME}</artifactId>|" pom.xml\n\
    sed -i -e "s|com.example.fn.HelloFunction|com.example.fn.${JAVA_NAME}|" Dockerfile\n\
    sed -i -e "s|com.example.fn.HelloFunction|com.example.fn.${JAVA_NAME}|" src/main/conf/reflection.json\n\
    sed -i -e "s|HelloFunction|${JAVA_NAME}|" src/main/java/com/example/fn/HelloFunction.java\n\
    mv src/main/java/com/example/fn/HelloFunction.java "src/main/java/com/example/fn/${JAVA_NAME}.java"\n\
    sed -i -e "s|HelloFunction|${JAVA_NAME}|" src/test/java/com/example/fn/HelloFunctionTest.java\n\
    mv src/test/java/com/example/fn/HelloFunctionTest.java "src/test/java/com/example/fn/${JAVA_NAME}Test.java"\n\
fi\n\
tar c src pom.xml func.init.yaml Dockerfile\n' > build_init_image.sh \
    && chmod 755 build_init_image.sh

CMD ["./build_init_image.sh"]
