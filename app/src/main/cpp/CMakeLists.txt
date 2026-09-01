cmake_minimum_required(VERSION 3.10)
project(randomx_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(RANDOMX_SOURCES
    randomx/aes_hash.cpp
    randomx/argon2_ref.c
    randomx/argon2_core.c
    randomx/argon2_ssse3.c
    randomx/argon2_avx2.c
    randomx/bytecode_machine.cpp
    randomx/cpu.cpp
    randomx/dataset.cpp
    randomx/soft_aes.cpp
    randomx/virtual_memory.c
    randomx/vm_interpreted.cpp
    randomx/allocator.cpp
    randomx/instruction.cpp
    randomx/randomx.cpp
    randomx/superscalar.cpp
    randomx/vm_compiled.cpp
    randomx/vm_interpreted_light.cpp
    randomx/blake2_generator.cpp
    randomx/instructions_portable.cpp
    randomx/reciprocal.c
    randomx/virtual_machine.cpp
    randomx/vm_compiled_light.cpp
    randomx/blake2/blake2b.c
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    list(APPEND RANDOMX_SOURCES
        randomx/jit_compiler_a64_static.S
        randomx/jit_compiler_a64.cpp
    )
    set_property(SOURCE randomx/jit_compiler_a64_static.S PROPERTY LANGUAGE C)
elseif(ANDROID_ABI STREQUAL "x86_64")
    list(APPEND RANDOMX_SOURCES
        randomx/assembly_generator_x86.cpp
        randomx/jit_compiler_x86.cpp
        randomx/jit_compiler_x86_static.S
    )
    set_property(SOURCE randomx/jit_compiler_x86_static.S PROPERTY LANGUAGE C)
    set_source_files_properties(randomx/argon2_ssse3.c PROPERTIES COMPILE_FLAGS "-mssse3")
    set_source_files_properties(randomx/argon2_avx2.c PROPERTIES COMPILE_FLAGS "-mavx2")
endif()

add_library(randomx_jni SHARED
    randomx-jni.cpp
    ${RANDOMX_SOURCES}
)

target_include_directories(randomx_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/randomx
)

target_link_libraries(randomx_jni
    log
)

target_link_options(randomx_jni PRIVATE
    "-Wl,-z,max-page-size=16384"
)

