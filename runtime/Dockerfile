FROM registry.oracledx.com/skeppare/serverjre:8
COPY target/runtime-*.jar target/dependency/*.jar /function/runtime/
WORKDIR /function
ENTRYPOINT ["java", "-cp", "app/*:runtime/*",  "com.fnproject.fn.runtime.EntryPoint"]
