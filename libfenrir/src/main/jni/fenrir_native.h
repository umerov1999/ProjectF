#ifndef fenrir_native_h
#define fenrir_native_h

#ifndef LOG_DISABLED

#include <android/log.h>

#define FENRIR_NATIVE_LOG_TAG "fenrir_native_lib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, FENRIR_NATIVE_LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, FENRIR_NATIVE_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, FENRIR_NATIVE_LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, FENRIR_NATIVE_LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...)
#define LOGD(...)
#define LOGE(...)
#define LOGV(...)
#endif

#ifndef max
#define max(x, y) ((x) > (y)) ? (x) : (y)
#endif
#ifndef min
#define min(x, y) ((x) < (y)) ? (x) : (y)
#endif

#ifndef SafeGetStringUTFChars
#define SafeGetStringUTFChars(env, str, is_copy) ((str) ? (env)->GetStringUTFChars(str, is_copy) : nullptr)
#endif

#ifndef GZIP_HEADER
#define GZIP_HEADER "\x1F\x8B\x08"
#define GZIP_HEADER_LENGTH 3
#endif

#ifndef MY_LZ4_HEADER
#define MY_LZ4_HEADER "\x02\x4C\x5A\x34"
#define MY_LZ4_HEADER_LENGTH 4
#endif

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wpragma-pack"
#pragma pack(push, 1)
struct MY_LZ4HDR_PUSH {
    uint8_t hdr[4];
    uint32_t size;
};
#pragma pack(pop)
#pragma clang diagnostic pop

#endif