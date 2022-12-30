#include <iostream>
#include <sstream>
#include <cstring>
#include <jni.h>
#include "fenrir_native.h"
#include "MpegTsBitStream.hpp"
#include "MpegTsDemux.hpp"

using namespace std;
extern "C" {
JNIEXPORT jboolean
Java_dev_ragnarok_fenrir_module_hls_TSDemuxer_unpack(JNIEnv *env, jobject, jstring input,
                                                     jstring output, jboolean info,
                                                     jboolean print_debug) {
    char const *inputString = SafeGetStringUTFChars(env, input, nullptr);
    char const *outputString = SafeGetStringUTFChars(env, output, nullptr);
    string inputPath = inputString;
    string outputPath = outputString;
    if (inputString != nullptr) {
        env->ReleaseStringUTFChars(input, inputString);
    }
    if (outputString != nullptr) {
        env->ReleaseStringUTFChars(output, outputString);
    }

    MpegTsBitStream bitStream(inputPath);
    MpegTsDemuxer demuxer(info);
    Packet packet{};

    bool ret = true;
    while (bitStream.good() && bitStream.GetPacket(packet)) {
        if (!demuxer.DecodePacket(packet, outputPath)) {
            ret = false;
            break;
        }
    }
    if (print_debug) {
        LOGE("%s", bitStream.errp.str().c_str());
        LOGE("%s", demuxer.errp.str().c_str());
        LOGI("%s", demuxer.wrn.str().c_str());
    }
    return ret;
}
}