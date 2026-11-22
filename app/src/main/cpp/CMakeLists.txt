# Native 3D Stage spatial engine for PixelMusic Audio Lab
cmake_minimum_required(VERSION 3.22.1)
project(mochystage LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(mochystage SHARED
    stage/splitter.cpp
    stage/hrtf.cpp
    stage/tail.cpp
    stage/engine.cpp
    stage/stage_jni.cpp
)
target_compile_options(mochystage PRIVATE -O3 -ffp-contract=fast -Wall -fvisibility=hidden)
target_link_options(mochystage PRIVATE "-Wl,-z,max-page-size=16384")
