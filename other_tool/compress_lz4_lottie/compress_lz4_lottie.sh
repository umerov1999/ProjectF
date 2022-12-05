#!/bin/bash
clang compress_lz4_lottie.cpp lz4.c -lstdc++ -static -static-libgcc -o compress_lz4_lottie.elf
