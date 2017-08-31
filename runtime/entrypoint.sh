#!/bin/sh

set -e

FUNCTION_ENTRYPOINT=${1}

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
JVM_ARGS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2 -XX:+UseSerialGC"

exec java \
    ${JVM_ARGS} \
    -cp app/*:runtime/* \
    com.fnproject.fn.runtime.EntryPoint ${FUNCTION_ENTRYPOINT}
