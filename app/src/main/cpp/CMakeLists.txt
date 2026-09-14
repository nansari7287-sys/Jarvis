cmake_minimum_required(VERSION 3.22.1)

project("jarvis_native_engine")

# Optimize for maximum processing speed
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

add_library(
        jarvis_native_engine
        SHARED
        native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        jarvis_native_engine
        ${log-lib}
)
