#include <jni.h>
#include <string>

#include <cinttypes>
#include <cstdlib>

#include <android/bitmap.h>

#include "libyuv/convert_argb.h"
#include "libyuv/rotate_argb.h"
#include "libyuv/convert.h"
#include "libyuv/planar_functions.h"
#include "fenrir_native.h"

static libyuv::RotationMode get_rotation_mode(int rotation) {
    libyuv::RotationMode mode = libyuv::kRotate0;
    switch (rotation) {
        case 0:
            mode = libyuv::kRotate0;
            break;
        case 90:
            mode = libyuv::kRotate90;
            break;
        case 180:
            mode = libyuv::kRotate180;
            break;
        case 270:
            mode = libyuv::kRotate270;
            break;
        default:
            break;
    }
    return mode;
}

extern "C" JNIEXPORT jbyteArray
Java_dev_ragnarok_fenrir_module_camerax_ImageToolsNative_nativeRotateBuffer(
        JNIEnv *env,
        jobject,
        jobject src,
        jint stride,
        jint width,
        jint height,
        jint rotation,
        jboolean flip) {

    auto *src_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src));
    size_t src_size = env->GetDirectBufferCapacity(src);
    if (!src_ptr) {
        return nullptr;
    }
    libyuv::RotationMode mode = get_rotation_mode(rotation);
    bool flip_wh = (mode == libyuv::kRotate90 || mode == libyuv::kRotate270);
    int rotated_stride = flip_wh ? height : width;

    auto *resultBuf = new uint8_t[src_size];
    if (libyuv::RotatePlane(src_ptr, stride, resultBuf, rotated_stride, width, height, mode)) {
        delete[] resultBuf;
        return nullptr;
    }
    if (flip) {
        auto *tmpBuf = resultBuf;
        resultBuf = new uint8_t[src_size];
        libyuv::MirrorPlane(tmpBuf, rotated_stride, resultBuf, rotated_stride, width, height);
        delete[] tmpBuf;
    }
    auto d = env->NewByteArray((int) src_size);
    env->SetByteArrayRegion(d, 0, (int) src_size,
                            reinterpret_cast<const jbyte *>(resultBuf));
    delete[] resultBuf;
    return d;
}