#!/bin/bash
clang -c -std=c17 -O3 lz4.c lz4hc.c
clang -O3 -std=c++2a compress_lz4.cpp lz4.o lz4hc.o -lstdc++ -static -static-libgcc -o compress_lz4.elf
rm lz4.o lz4hc.o
strip compress_lz4.elf
