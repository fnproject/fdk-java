# Native components for Fn unix socket protocol

This is a very simple JNI binding to expose unix sockets to the Fn runtime

## Building

you can rebuild a linux version (for the FDK itself) of the JNI library using `./rebuild_so.sh` this runs `buildit.sh` in a suitable docker container

For testing on a mac you can also compile locally by running `buildit.sh`, you will need at least:

* XCode compiler toolchain
* cmake
* make
* a JDK installed (for cmake JNI)


Current issues:
* This is using old-style JNI array passing which is slow - it should be using native buffers
* Doesn't support non-blocking operations, specifically reads and writes which will block indefinitely