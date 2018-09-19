cmake_minimum_required(VERSION 2.8)
project(fnunixsocket)
set(CMAKE_BUILD_TYPE Release)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")


add_library(fnunixsocket SHARED unix_socket.c)
