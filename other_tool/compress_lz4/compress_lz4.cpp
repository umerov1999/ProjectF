#include <iostream>
#include <list>
#include <vector>
#include <cstring>
#include <fcntl.h>
#include "lz4.h"
#include "lz4hc.h"
#include <cstdio>
#ifndef WINDOWS_PLATFORM
#include <unistd.h>
#endif
using namespace std;

#define HEADER "\x02\x4C\x5A\x34"
#pragma pack(push, 1)
struct HDR
{
	uint8_t hdr[4];
	uint32_t size;
};
#pragma pack(pop)

static string get_content(const string& file) {
	FILE* fl = fopen(file.c_str(), "rb");
	if (fl != NULL)
	{
		string jsonS;
		fseek(fl, 0, SEEK_END);
		jsonS.resize(ftell(fl));
		fseek(fl, 0, SEEK_SET);
		fread((char*)jsonS.data(), 1, jsonS.size(), fl);
		fclose(fl);
		return jsonS;
	}
	cout << "can't open " << file << endl;
#ifdef WINDOWS_PLATFORM
	_exit(1);
#else
	exit(1);
#endif
}

static void write_content(const string& file, const string& data, int size) {
	FILE* fl = fopen(file.c_str(), "wb");
	if (fl != NULL)
	{
		fwrite(data.c_str(), 1, size, fl);
		fclose(fl);
		return;
	}
	cout << "can't open " << file << endl;
#ifdef WINDOWS_PLATFORM
	_exit(1);
#else
	exit(1);
#endif
}

void work(const string& flarg) {
	string fl = flarg;
	size_t ps;
	if ((ps = fl.find("_lz4")) == string::npos) {
		string in = get_content(fl);
#ifdef WINDOWS_PLATFORM
		_unlink(fl.c_str());
#else
		unlink(fl.c_str());
#endif
		fl += "_lz4";
		std::string out;
		auto cnt = LZ4_compressBound((int)in.size());
		HDR hdr = {};
		memcpy(hdr.hdr, HEADER, strlen(HEADER));
		hdr.size = (int)in.size();

		out.resize(cnt + sizeof(HDR));
		auto size = (uint32_t)LZ4_compress_HC(in.data(), ((char*)out.data() + sizeof(HDR)),
			(int)in.size(), cnt, LZ4HC_CLEVEL_MAX);
		memcpy((char*)out.data(), &hdr, sizeof(HDR));
		write_content(fl, out, (int)size + sizeof(HDR));
	}
	else {
		string in = get_content(fl);
#ifdef WINDOWS_PLATFORM
		_unlink(fl.c_str());
#else
		unlink(fl.c_str());
#endif
		fl = fl.replace(ps, 4, "");
		HDR hdr = {};
		memcpy(&hdr, in.data(), sizeof(HDR));
		std::string out;
		out.resize(hdr.size);
		LZ4_decompress_safe(((const char*)in.data() + sizeof(HDR)), (char*)out.data(),
			(int)in.size() - sizeof(HDR), hdr.size);
		write_content(fl, out, (int)out.length());
	}
	cout << fl << endl;
}

int main(int argc, char* argv[]) {
	locale::global(locale("ru_RU.UTF-8"));
	if (argc < 2) {
		cout << "\ncompress_lz4 - usage is: \n\n      compress_lz4 <sources>\n\n";
		return 1;
	}
	for (int i = 1; i < argc; i++) {
		work(argv[i]);
	}
	cout << "Copyright (c) Umerov Artem, 2024" << endl;
	return 0;
}
