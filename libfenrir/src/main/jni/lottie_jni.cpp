#include <jni.h>
#include <android/bitmap.h>
#include <rlottie.h>
#include <lz4.h>
#include <zlib.h>
#include <unistd.h>
#include <fstream>
#include <map>
#include <sys/stat.h>
#include <utime.h>
#include "fenrir_native.h"
#include "gif.h"

using namespace rlottie;

class LottieInfo {
public:
    std::unique_ptr<Animation> animation;
    size_t frameCount = 0;
    std::string path;
};

std::string doDecompressResource(size_t length, char *bytes, bool &orig) {
    orig = false;
    std::string data;
    if (length >= GZIP_HEADER_LENGTH && memcmp(bytes, GZIP_HEADER, GZIP_HEADER_LENGTH) == 0) {
        z_stream zs;
        memset(&zs, 0, sizeof(zs));

        if (inflateInit2(&zs, 15 + 16) != Z_OK) {
            return "";
        }

        zs.next_in = (Bytef *) bytes;
        zs.avail_in = length;

        int ret;
        std::vector<char> outBuffer(32768);

        do {
            zs.next_out = reinterpret_cast<Bytef *>(outBuffer.data());
            zs.avail_out = outBuffer.size();
            ret = inflate(&zs, 0);
            if (data.size() < zs.total_out) {
                data.append(outBuffer.data(), zs.total_out - data.size());
            }

        } while (ret == Z_OK);
        inflateEnd(&zs);
        if (ret != Z_STREAM_END) {
            return "";
        }
    } else if (length >= MY_LZ4_HEADER_LENGTH &&
               memcmp(bytes, MY_LZ4_HEADER, MY_LZ4_HEADER_LENGTH) == 0) {
        MY_LZ4HDR_PUSH hdr = {};
        memcpy(&hdr, bytes, sizeof(MY_LZ4HDR_PUSH));
        data.resize(hdr.size);
        LZ4_decompress_safe(((const char *) bytes + sizeof(MY_LZ4HDR_PUSH)), (char *) data.data(),
                            (int) length - (int) sizeof(MY_LZ4HDR_PUSH), (int) hdr.size);
    } else {
        orig = true;
    }
    return data;
}

extern "C" {
JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_create(JNIEnv *env, jobject, jstring src,
                                                               jintArray data,
                                                               jintArray colorReplacement,
                                                               jboolean useMoveColor) {
    auto *info = new LottieInfo();
    internal::ColorReplace *colors = nullptr;
    if (colorReplacement != nullptr) {
        jint *arr = env->GetIntArrayElements(colorReplacement, nullptr);
        if (arr != nullptr) {
            jsize len = env->GetArrayLength(colorReplacement);
            if (len % 2 == 0) {
                colors = new internal::ColorReplace(useMoveColor);
                for (int32_t a = 0; a < len / 2; a++) {
                    colors->colorMap[arr[a * 2]] = arr[a * 2 + 1];
                }
            }
            env->ReleaseIntArrayElements(colorReplacement, arr, 0);
        }
    }

    char const *srcString = SafeGetStringUTFChars(env, src, nullptr);
    info->path = srcString;
    if (srcString != nullptr) {
        env->ReleaseStringUTFChars(src, srcString);
    }
    std::ifstream f;
    f.open(info->path);
    if (!f.is_open()) {
        delete info;
        return 0;
    }
    f.seekg(0, std::ios::end);
    auto length = f.tellg();
    f.seekg(0, std::ios::beg);
    if (length <= 0) {
        f.close();
        delete info;
        return 0;
    }
    auto *arr = new char[(size_t) length + 1];
    f.read(arr, length);
    f.close();
    arr[length] = '\0';
    bool orig;
    std::string jsonString = doDecompressResource(length, arr, orig);
    if (orig) {
        info->animation = rlottie::Animation::loadFromData(arr, colors);
    }
    delete[] arr;
    if (!orig) {
        if (jsonString.length() <= 0) {
            delete info;
            return 0;
        }
        info->animation = rlottie::Animation::loadFromData(jsonString.c_str(), colors);
    }
    if (info->animation == nullptr) {
        delete info;
        return 0;
    }
    info->frameCount = info->animation->totalFrame();
    /*
    if (info->animation->frameRate() > 60 || info->frameCount > 600) {
        delete info;
        return 0;
    }
    */

    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    if (dataArr != nullptr) {
        dataArr[0] = (jint) info->frameCount;
        dataArr[1] = (jint) info->animation->frameRate();
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }
    return (jlong) (intptr_t) info;
}

JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_createWithJson(JNIEnv *env, jobject,
                                                                       jlong json,
                                                                       jintArray data,
                                                                       jintArray colorReplacement,
                                                                       jboolean useMoveColor) {
    internal::ColorReplace *colors = nullptr;
    if (colorReplacement != nullptr) {
        jint *arr = env->GetIntArrayElements(colorReplacement, nullptr);
        if (arr != nullptr) {
            jsize len = env->GetArrayLength(colorReplacement);
            if (len % 2 == 0) {
                colors = new internal::ColorReplace(useMoveColor);
                for (int32_t a = 0; a < len / 2; a++) {
                    colors->colorMap[arr[a * 2]] = arr[a * 2 + 1];
                }
            }
            env->ReleaseIntArrayElements(colorReplacement, arr, 0);
        }
    }

    auto *info = new LottieInfo();
    auto u = ((std::vector<char> *) (intptr_t) json);
    bool orig;
    std::string jsonString = doDecompressResource(u->size(), u->data(), orig);
    if (orig) {
        info->animation = rlottie::Animation::loadFromData(u->data(), colors);
    } else {
        info->animation = rlottie::Animation::loadFromData(jsonString.c_str(), colors);
    }
    if (info->animation == nullptr) {
        delete info;
        return 0;
    }
    info->frameCount = info->animation->totalFrame();

    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    if (dataArr != nullptr) {
        dataArr[0] = (int) info->frameCount;
        dataArr[1] = (int) info->animation->frameRate();
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }
    return (jlong) (intptr_t) info;
}

JNIEXPORT void
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_destroy(JNIEnv *, jobject, jlong ptr) {
    if (!ptr) {
        return;
    }
    auto *info = (LottieInfo *) (intptr_t) ptr;
    delete info;
}

JNIEXPORT void
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_setLayerColor(JNIEnv *env, jobject,
                                                                      jlong ptr,
                                                                      jstring layer, jint color) {
    if (!ptr || layer == nullptr) {
        return;
    }
    auto *info = (LottieInfo *) (intptr_t) ptr;
    char const *layerString = SafeGetStringUTFChars(env, layer, nullptr);
    Color v((float) ((color) & 0xff) / 255.0f,
            (float) ((color >> 8) & 0xff) /
            255.0f,
            (float) ((color >> 16) & 0xff) /
            255.0f);

    info->animation->setValue<Property::FillColor>(layerString, v);
    info->animation->setValue<Property::StrokeColor>(layerString, v);
    if (layerString != nullptr) {
        env->ReleaseStringUTFChars(layer, layerString);
    }
}

JNIEXPORT void
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_replaceColors(JNIEnv *env, jobject,
                                                                      jlong ptr,
                                                                      jintArray colorReplacement) {
    if (!ptr || colorReplacement == nullptr) {
        return;
    }
    auto *info = (LottieInfo *) (intptr_t) ptr;
    if (!info->animation->colorMap || info->animation->colorMap->useMoveColor) {
        return;
    }
    jint *arr = env->GetIntArrayElements(colorReplacement, nullptr);
    if (arr != nullptr) {
        jsize len = env->GetArrayLength(colorReplacement);
        if (len % 2 == 0) {
            for (int32_t a = 0; a < len / 2; a++) {
                info->animation->colorMap->colorMap[arr[a * 2]] = arr[a * 2 + 1];
            }
            info->animation->resetCurrentFrame();
        }
        env->ReleaseIntArrayElements(colorReplacement, arr, 0);
    }
}

JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_rlottie_RLottieDrawable_getFrame(JNIEnv *env, jobject, jlong ptr,
                                                                 jint frame,
                                                                 jobject bitmap, jint w, jint h,
                                                                 jint stride,
                                                                 jboolean clear) {
    if (!ptr || bitmap == nullptr) {
        return 0;
    }
    auto *info = (LottieInfo *) (intptr_t) ptr;

    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        Surface surface((uint32_t *) pixels, (size_t) w, (size_t) h, (size_t) stride);
        info->animation->renderSync((size_t) frame, surface, clear);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
    return frame;
}

class Lottie2Gif {
public:
    static bool render(LottieInfo *player, jobject bitmap, int w, int h, int stride, int bgColor,
                       bool transparent, const std::string &gifName, int bitDepth,
                       bool dither, JNIEnv *env, jobject listener) {
        void *pixels;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
            size_t frameCount = player->animation->totalFrame();
            //auto pixels = std::unique_ptr<uint32_t[]>(new uint32_t[w * h]);

            uint32_t delay = 2;
            GifBuilder builder(gifName, w, h, bgColor, delay, bitDepth, dither);
            size_t start = 0, end = frameCount;

            if (listener != nullptr) {
                jweak store_Wlistener = env->NewWeakGlobalRef(listener);
                jclass clazz = env->GetObjectClass(store_Wlistener);

                jmethodID mth_update = env->GetMethodID(clazz, "onProgress", "(II)V");
                jmethodID mth_start = env->GetMethodID(clazz, "onStarted", "()V");
                jmethodID mth_end = env->GetMethodID(clazz, "onFinished", "()V");

                env->CallVoidMethod(store_Wlistener, mth_start);

                for (size_t i = start; i < end; i++) {
                    rlottie::Surface surface((uint32_t *) pixels, (size_t) w, (size_t) h,
                                             (size_t) stride);
                    player->animation->renderSync(i, surface, true);
                    builder.addFrame(surface, transparent, delay, bitDepth, dither);

                    env->CallVoidMethod(store_Wlistener, mth_update, (jint) (i + 1),
                                        (jint) frameCount);
                }

                env->CallVoidMethod(store_Wlistener, mth_end);
            } else {
                for (size_t i = start; i < end; i++) {
                    rlottie::Surface surface((uint32_t *) pixels, (size_t) w, (size_t) h,
                                             (size_t) stride);
                    player->animation->renderSync(i, surface, true);
                    builder.addFrame(surface, transparent, delay, bitDepth, dither);
                }
            }

            AndroidBitmap_unlockPixels(env, bitmap);
            return true;
        }
        return false;
    }

};

JNIEXPORT
jboolean
Java_dev_ragnarok_fenrir_module_rlottie_RLottie2Gif_lottie2gif(JNIEnv *env, jobject, jlong json,
                                                               jobject bitmap, jint w, jint h,
                                                               jint stride, jint bgColor,
                                                               jboolean transparent,
                                                               jstring gifName,
                                                               jint bitDepth,
                                                               jboolean dither,
                                                               jobject listener) {
    if (!json) {
        return false;
    }
    auto *info = new LottieInfo();
    auto u = ((std::vector<char> *) (intptr_t) json);
    bool orig;
    std::string jsonString = doDecompressResource(u->size(), u->data(), orig);
    if (orig) {
        info->animation = rlottie::Animation::loadFromData(u->data(), nullptr);
    } else {
        info->animation = rlottie::Animation::loadFromData(jsonString.c_str(), nullptr);
    }
    if (info->animation == nullptr) {
        delete info;
        return 0;
    }
    info->frameCount = info->animation->totalFrame();

    char const *name = SafeGetStringUTFChars(env, gifName, nullptr);
    return Lottie2Gif::render(info, bitmap, w, h, stride, bgColor, (bool) transparent, name,
                              bitDepth, dither, env, listener);
}

}
