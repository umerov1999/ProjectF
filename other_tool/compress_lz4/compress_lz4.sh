#!/bin/bash
clang compress_lz4.cpp lz4.c lz4hc.c -lstdc++ -static -static-libgcc -o compress_lz4.elf