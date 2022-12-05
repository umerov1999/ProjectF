#ifndef tools_h
#define tools_h

#include <unistd.h>
#include <iostream>
#include <sstream>

void crc64(uint64_t &crc, const void *s, int l);

std::string dumpCrc64(uint64_t crc);

int crc32(const std::string &str);

#endif
