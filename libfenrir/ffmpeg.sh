#!/bin/bash
SCRIPT_DIR=${PWD}
cd ~/
git clone https://git.videolan.org/git/ffmpeg.git
cd ffmpeg
git checkout release/6.0
rm -r -f ".git"

ENABLED_DECODERS=(gif mpeg4 h264 hevc mp3 aac ac3 eac3 flac vorbis alac)
HOST_PLATFORM="linux-x86_64"
NDK_PATH="/home/umerov/Android/Sdk/ndk/26.0.10792818"

echo 'Please input platform version (Example 21 - Android 5.0): '
read ANDROID_PLATFORM

cd ${SCRIPT_DIR}/src/main/jni/
./build_ffmpeg.sh "${NDK_PATH}" "${HOST_PLATFORM}" "${ANDROID_PLATFORM}" "-fvisibility=hidden" "${ENABLED_DECODERS[@]}"
