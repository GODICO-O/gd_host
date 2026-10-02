cmake_minimum_required(VERSION 3.22.1)
project("launcherhook")

add_library(
        launcherhook
        SHARED
        native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        launcherhook
        ${log-lib}
)
