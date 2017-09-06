FROM fnproject/fn-java-fdk:jrebase
COPY target/runtime-*.jar target/dependency/*.jar /function/runtime/
COPY entrypoint.sh /entrypoint.sh

WORKDIR /function
ENTRYPOINT ["/entrypoint.sh"]
