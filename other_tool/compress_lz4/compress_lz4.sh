#!/bin/bash
clang -m64 -fPIC -c -static -std=c17 -O3 lz4.c lz4hc.c
clang++ -m64 -fPIC -static -std=c++2a -stdlib=libc++ -O3 compress_lz4.cpp lz4.o lz4hc.o -o compress_lz4.elf
rm lz4.o lz4hc.o
strip compress_lz4.elf
