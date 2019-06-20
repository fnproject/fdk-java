FROM openjdk:8-jre-slim
COPY target/runtime-*.jar target/dependency/*.jar /function/runtime/
COPY src/main/c/libfnunixsocket.so /function/runtime/lib/

RUN ["java", "-Xshare:dump"]

RUN addgroup --system --gid 1000 fn && adduser --uid 1000 --gid 1000 fn

# UseCGroupMemoryLimitForHeap looks up /sys/fs/cgroup/memory/memory.limit_in_bytes inside the container to determine
# what the heap should be set to. This is an experimental feature at the moment, thus we need to unlock to use it.
#
# MaxRAMFraction is used modify the heap size and it is used as a denominator where the numerator is phys_mem.
# It seems that this value is a uint in the JVM code, thus can only specify 1 => 100%, 2 => 50%, 3 => 33.3%, 4 => 25%
# and so on.
#
# SerialGC is used here as it's likely that we'll be running many JVMs on the same host machine and it's also likely
# that the number of JVMs will outnumber the number of available processors.
#
# The max memory value obtained with these args seem to be okay for most memory limits. The exception is when the
# memory limit is set to 128MiB, in which case maxMemory returns roughly half.
ENTRYPOINT [ "java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:-UsePerfData", "-XX:MaxRAMFraction=2", "-XX:+UseSerialGC", "-Xshare:on", "-Djava.library.path=/function/runtime/lib", "-cp", "/function/app/*:/function/runtime/*", "com.fnproject.fn.runtime.EntryPoint" ]
