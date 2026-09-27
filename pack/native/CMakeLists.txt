cmake_minimum_required(VERSION 3.22.1)

project(yume-loader C)

include(FetchContent)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(XZ_NLS OFF CACHE BOOL "" FORCE)
set(XZ_DOC OFF CACHE BOOL "" FORCE)
set(XZ_TOOL_XZ OFF CACHE BOOL "" FORCE)
set(XZ_TOOL_XZDEC OFF CACHE BOOL "" FORCE)
set(XZ_TOOL_LZMADEC OFF CACHE BOOL "" FORCE)
set(XZ_TOOL_LZMAINFO OFF CACHE BOOL "" FORCE)
set(XZ_TOOL_SCRIPTS OFF CACHE BOOL "" FORCE)
set(XZ_ENCODERS "" CACHE STRING "" FORCE)
set(XZ_DECODERS "lzma1;lzma2" CACHE STRING "" FORCE)
set(XZ_MICROLZMA_ENCODER OFF CACHE BOOL "" FORCE)
set(XZ_MICROLZMA_DECODER OFF CACHE BOOL "" FORCE)
set(XZ_LZIP_DECODER OFF CACHE BOOL "" FORCE)
set(XZ_THREADS no CACHE STRING "" FORCE)
set(XZ_MATCH_FINDERS "" CACHE STRING "" FORCE)

# Section-per-function so the loader link can drop xz encoders and unused checks.
add_compile_options(-ffunction-sections -fdata-sections)

FetchContent_Declare(
    xz
    URL https://github.com/tukaani-project/xz/releases/download/v5.8.1/xz-5.8.1.tar.gz
    URL_HASH SHA256=507825b599356c10dca1cd720c9d0d0c9d5400b9de300af00e4d1ea150795543
)
FetchContent_MakeAvailable(xz)

if(TARGET liblzma)
    # -O2 keeps the decompress loop fast. Sections, hidden visibility and no unwind
    # tables let --gc-sections and --icf drop the unused xz encoder without slowing it.
    target_compile_options(liblzma PRIVATE
        -O2
        -ffunction-sections
        -fdata-sections
        -fvisibility=hidden
        -fno-unwind-tables
        -fno-asynchronous-unwind-tables)
endif()

add_library(loader SHARED loader.c)
target_compile_features(loader PRIVATE c_std_17)
target_compile_options(loader PRIVATE
    -Os
    -fvisibility=hidden
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -Wall
    -Wextra
    -Werror)
target_include_directories(loader PRIVATE "${xz_SOURCE_DIR}/src/liblzma/api")
target_link_options(loader PRIVATE
    -Wl,--gc-sections
    -Wl,--icf=safe
    -Wl,--exclude-libs,ALL
    -Wl,-s
    -Wl,-soname,libloader.so)
target_link_libraries(loader PRIVATE liblzma)
