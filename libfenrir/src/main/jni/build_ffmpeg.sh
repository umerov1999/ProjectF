#!/bin/bash

NDK_PATH=$1
HOST_PLATFORM=$2
ANDROID_PLATFORM=$3
EXTRA_C_FLAGS=$4
ENABLED_DECODERS=("${@:5}")
JOBS=$(nproc 2> /dev/null || sysctl -n hw.ncpu 2> /dev/null || echo 4)
echo "Using $JOBS jobs for make"
COMMON_OPTIONS="
    --target-os=android
    --enable-cross-compile
    --enable-static
    --enable-stripping
    --enable-pic
    --disable-shared
    --disable-doc
    --disable-avx
    --disable-programs
    --disable-everything
    --disable-avdevice
    --disable-postproc
    --disable-avfilter
    --disable-symver
    --disable-network
    --disable-zlib
    --disable-debug
    --disable-vulkan
    --enable-demuxer=gif
    --enable-demuxer=mov
    --enable-demuxer=mp3
    --enable-demuxer=flac
    --enable-demuxer=ogg
    --enable-muxer=mp4
    --enable-encoder=aac
    --enable-swresample
    --enable-protocol=file
    --enable-nonfree
    --enable-runtime-cpudetect
    --enable-pthreads
    --enable-swscale
    --enable-hwaccels
    "

TOOLCHAIN_PREFIX="${NDK_PATH}/toolchains/llvm/prebuilt/${HOST_PLATFORM}/bin"
for decoder in "${ENABLED_DECODERS[@]}"
do
    COMMON_OPTIONS="${COMMON_OPTIONS} --enable-decoder=${decoder}"
done
cd "/home/umerov/ffmpeg"
./configure \
    --libdir=android-libs/armeabi-v7a \
    --arch=arm \
    --cpu=armv7-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/armv7a-linux-androideabi${ANDROID_PLATFORM}-" \
    --nm="${TOOLCHAIN_PREFIX}/llvm-nm" \
    --ar="${TOOLCHAIN_PREFIX}/llvm-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/llvm-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/llvm-strip" \
    --extra-cflags="-marm -march=armv7-a $EXTRA_C_FLAGS" \
    --enable-neon \
    --enable-asm \
    --enable-inline-asm \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
./configure \
    --libdir=android-libs/arm64-v8a \
    --arch=aarch64 \
    --cpu=armv8-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/aarch64-linux-android${ANDROID_PLATFORM}-" \
    --nm="${TOOLCHAIN_PREFIX}/llvm-nm" \
    --ar="${TOOLCHAIN_PREFIX}/llvm-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/llvm-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/llvm-strip" \
    --extra-cflags="$EXTRA_C_FLAGS" \
    --enable-neon \
    --enable-optimizations \
    --enable-asm \
    --enable-inline-asm \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
./configure \
    --libdir=android-libs/x86_64 \
    --arch=x86_64 \
    --cpu=x86_64 \
    --cross-prefix="${TOOLCHAIN_PREFIX}/x86_64-linux-android${ANDROID_PLATFORM}-" \
    --nm="${TOOLCHAIN_PREFIX}/llvm-nm" \
    --ar="${TOOLCHAIN_PREFIX}/llvm-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/llvm-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/llvm-strip" \
    --extra-cflags="$EXTRA_C_FLAGS" \
    --disable-mmx \
    --disable-inline-asm \
    --disable-asm \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
