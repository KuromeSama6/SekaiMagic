cmake_minimum_required(VERSION 3.10.2)

# Define the project
project("uinput_touch")

# Specify the source files
add_library(uinput_touch SHARED uinput_touch.c)

# Specify the include directories for your C code
include_directories(src/main/cpp)

# Link libraries (e.g., to system libraries like uinput)
find_library(log-lib log)  # Android log library for logging
target_link_libraries(uinput_touch ${log-lib})
