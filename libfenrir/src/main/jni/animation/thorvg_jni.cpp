#include <jni.h>
#include <android/bitmap.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <memory>
#include <vector>
#include <thread>
#include <map>
#include <utime.h>
#include <thorvg.h>
#include <lz4.h>
#include <zlib.h>
#include <sys/stat.h>
#include "fenrir_native.h"
#include "tvgGifEncoder.h"

class LottieAnimation {
public:
    ~LottieAnimation() {
        delete animation;
        delete canvas;
    }

    tvg::Animation *animation = nullptr;
    tvg::SwCanvas *canvas = nullptr;
    bool isCanvasPushed = false;
};

static pthread_mutex_t *lockMutex = nullptr;
static std::map<std::string, uint32_t> customColorsTable;

extern std::string doDecompressResource(size_t length, char *bytes, bool &orig);

extern bool fenrirNativeThorVGInited;

void getCustomColorSVG(const std::string &name, uint8_t *r, uint8_t *g, uint8_t *b) {
    if (!lockMutex) {
        lockMutex = new pthread_mutex_t();
        pthread_mutex_init(lockMutex, nullptr);
    }
    pthread_mutex_lock(lockMutex);
    uint32_t clr = customColorsTable[name];
    pthread_mutex_unlock(lockMutex);
    *r = (((uint8_t *) (&(clr)))[2]);
    *g = (((uint8_t *) (&(clr)))[1]);
    *b = (((uint8_t *) (&(clr)))[0]);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGSVGRender_registerColorsNative(JNIEnv *env,
                                                                                      jobject,
                                                                                      jstring name,
                                                                                      jint value) {
    if (!fenrirNativeThorVGInited || !name) {
        return;
    }
    char const *nameString = SafeGetStringUTFChars(env, name, nullptr);
    if (!lockMutex) {
        lockMutex = new pthread_mutex_t();
        pthread_mutex_init(lockMutex, nullptr);
    }
    if (nameString != nullptr) {
        pthread_mutex_lock(lockMutex);
        customColorsTable["[|" + std::string(nameString) + "|]"] = value;
        pthread_mutex_unlock(lockMutex);
        env->ReleaseStringUTFChars(name, nameString);
    }
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGSVGRender_createBitmapNative(JNIEnv *env,
                                                                                    jobject,
                                                                                    jlong res,
                                                                                    jobject bitmap,
                                                                                    jint w,
                                                                                    jint h) {
    if (!bitmap) {
        return;
    }
    auto u = reinterpret_cast<std::vector<char> *>(res);
    auto canvas = tvg::SwCanvas::gen();
    if (!canvas) {
        return;
    }
    canvas->mempool(tvg::SwCanvas::Individual);

    auto picture = tvg::Picture::gen();
    bool orig;
    std::string jsonString = doDecompressResource(u->size(), u->data(), orig);
    tvg::Result result = orig ? picture->load((const char *) u->data(), u->size(), "svg", nullptr,
                                              true)
                              : picture->load((const char *) jsonString.data(), jsonString.size(),
                                              "svg", nullptr, true);
    if (result != tvg::Result::Success) {
        return;
    }
    float scale;
    float shiftX = 0.0f, shiftY = 0.0f;
    float w2, h2;
    picture->size(&w2, &h2);

    if (h >= w) {
        if (w2 >= h2) {
            scale = (float) w / w2;
            shiftY = ((float) h - h2 * scale) * 0.5f;
        } else {
            scale = (float) h / h2;
            shiftX = ((float) w - w2 * scale) * 0.5f;
        }
    } else {
        if (w2 < h2) {
            scale = (float) w / w2;
            shiftY = ((float) h - h2 * scale) * 0.5f;
        } else {
            scale = (float) h / h2;
            shiftX = ((float) w - w2 * scale) * 0.5f;
        }
    }

    picture->scale(scale);
    picture->translate(shiftX, shiftY);

    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        if (canvas->target((uint32_t *) pixels, w, w, h, tvg::ColorSpace::ABGR8888) !=
            tvg::Result::Success) {
            AndroidBitmap_unlockPixels(env, bitmap);
            return;
        }

        canvas->push(picture);
        if (canvas->draw() == tvg::Result::Success) {
            canvas->sync();
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

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

extern "C" JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottieDrawable_nLoadFromFile(JNIEnv *env,
                                                                                    jobject,
                                                                                    jstring srcPath,
                                                                                    jintArray data,
                                                                                    jintArray colorReplacement,
                                                                                    jboolean useMoveColor) {
    auto *info = new LottieAnimation();
    tvg::ColorReplace colors;
    if (useMoveColor) {
        colors.setUseCustomColorsLottieOffset();
    }
    if (colorReplacement != nullptr) {
        jint *arr = env->GetIntArrayElements(colorReplacement, nullptr);
        if (arr != nullptr) {
            jsize len = env->GetArrayLength(colorReplacement);
            if (len % 2 == 0) {
                for (int32_t a = 0; a < len / 2; a++) {
                    colors.registerCustomColorLottie(arr[a * 2], arr[a * 2 + 1]);
                }
            }
            env->ReleaseIntArrayElements(colorReplacement, arr, 0);
        }
    }

    char const *srcString = SafeGetStringUTFChars(env, srcPath, nullptr);
    std::string path = srcString;
    if (srcString != nullptr) {
        env->ReleaseStringUTFChars(srcPath, srcString);
    }
    std::ifstream f;
    f.open(path);
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
        info->animation = tvg::Animation::gen();
        info->animation->picture()->load(arr, length, "lottie", nullptr, true, &colors);
    }
    delete[] arr;
    if (!orig) {
        if (jsonString.empty()) {
            delete info;
            return 0;
        }
        info->animation = tvg::Animation::gen();
        info->animation->picture()->load(jsonString.data(), jsonString.size(), "lottie", nullptr,
                                         true, &colors);
    }
    info->canvas = tvg::SwCanvas::gen();
    float tmpWidth = 0;
    float tmpHeight = 0;
    info->animation->picture()->size(&tmpWidth, &tmpHeight);

    if (!info->animation || !info->canvas || tmpWidth < 1 || tmpHeight < 1) {
        delete info;
        return 0;
    }
    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    if (dataArr != nullptr) {
        dataArr[0] = (jint) info->animation->totalFrame();
        dataArr[1] = (jint) info->animation->duration();
        dataArr[2] = (jint) tmpWidth;
        dataArr[3] = (jint) tmpHeight;
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }

    return reinterpret_cast<jlong>(info);
}

extern "C" JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottieDrawable_nLoadFromMemory(JNIEnv *env,
                                                                                      jobject,
                                                                                      jlong json,
                                                                                      jintArray data,
                                                                                      jintArray colorReplacement,
                                                                                      jboolean useMoveColor) {
    tvg::ColorReplace colors;
    if (useMoveColor) {
        colors.setUseCustomColorsLottieOffset();
    }
    if (colorReplacement != nullptr) {
        jint *arr = env->GetIntArrayElements(colorReplacement, nullptr);
        if (arr != nullptr) {
            jsize len = env->GetArrayLength(colorReplacement);
            if (len % 2 == 0) {
                for (int32_t a = 0; a < len / 2; a++) {
                    colors.registerCustomColorLottie(arr[a * 2], arr[a * 2 + 1]);
                }
            }
            env->ReleaseIntArrayElements(colorReplacement, arr, 0);
        }
    }

    auto *info = new LottieAnimation();
    auto u = reinterpret_cast<std::vector<char> *>(json);
    bool orig;
    std::string jsonString = doDecompressResource(u->size(), u->data(), orig);
    if (orig) {
        info->animation = tvg::Animation::gen();
        info->animation->picture()->load(u->data(), u->size(), "lottie", nullptr, true, &colors);
    } else {
        info->animation = tvg::Animation::gen();
        info->animation->picture()->load(jsonString.data(), jsonString.size(), "lottie", nullptr,
                                         true,
                                         &colors);
    }
    info->canvas = tvg::SwCanvas::gen();
    float tmpWidth = 0;
    float tmpHeight = 0;
    info->animation->picture()->size(&tmpWidth, &tmpHeight);

    if (!info->animation || !info->canvas || tmpWidth < 1 || tmpHeight < 1) {
        delete info;
        return 0;
    }
    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    if (dataArr != nullptr) {
        dataArr[0] = (jint) info->animation->totalFrame();
        dataArr[1] = (jint) info->animation->duration();
        dataArr[2] = (jint) tmpWidth;
        dataArr[3] = (jint) tmpHeight;
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }
    return reinterpret_cast<jlong>(info);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottieDrawable_nDestroy(JNIEnv *, jobject,
                                                                               jlong ptr) {
    if (!ptr) {
        return;
    }
    auto *info = reinterpret_cast<LottieAnimation *>(ptr);
    delete info;
}

extern "C" void
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottieDrawable_nSetBufferSize(
        JNIEnv *env, jobject,
        jlong ptr, jobject bitmap, jfloat width, jfloat height) {
    if (!ptr || !bitmap) {
        return;
    }

    auto *info = reinterpret_cast<LottieAnimation *>(ptr);
    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        info->canvas->sync();
        info->canvas->target((uint32_t *) pixels, (uint32_t) width, (uint32_t) width,
                             (uint32_t) height,
                             tvg::ColorSpace::ABGR8888);

        float scale;
        float shiftX = 0.0f, shiftY = 0.0f;
        float w2, h2;
        info->animation->picture()->size(&w2, &h2);

        if (height >= width) {
            if (w2 >= h2) {
                scale = width / w2;
                shiftY = (height - h2 * scale) * 0.5f;
            } else {
                scale = height / h2;
                shiftX = (width - w2 * scale) * 0.5f;
            }
        } else {
            if (w2 < h2) {
                scale = width / w2;
                shiftY = (height - h2 * scale) * 0.5f;
            } else {
                scale = height / h2;
                shiftX = (width - w2 * scale) * 0.5f;
            }
        }

        info->animation->picture()->scale(scale);
        info->animation->picture()->translate(shiftX, shiftY);

        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottieDrawable_nGetFrame(JNIEnv *env,
                                                                                jobject,
                                                                                jlong ptr,
                                                                                jobject bitmap,
                                                                                jint frame) {
    if (!ptr || !bitmap) {
        return;
    }
    auto *info = reinterpret_cast<LottieAnimation *>(ptr);
    if (!info->canvas) {
        return;
    }

    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        info->animation->frame((float) frame);
        if (!info->isCanvasPushed) {
            info->isCanvasPushed = true;
            info->canvas->push(info->animation->picture());
        } else {
            info->canvas->update(info->animation->picture());
        }
        if (info->canvas->draw(true) == tvg::Result::Success) {
            info->canvas->sync();
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" JNIEXPORT
jboolean
Java_dev_ragnarok_fenrir_module_animation_thorvg_ThorVGLottie2Gif_lottie2gif(JNIEnv *env, jobject,
                                                                             jstring srcPath,
                                                                             jint w, jint h,
                                                                             jint bgColor,
                                                                             jboolean transparent,
                                                                             jint fps,
                                                                             jstring gifName,
                                                                             jobject listener) {
    auto info = LottieAnimation();
    char const *srcString = SafeGetStringUTFChars(env, srcPath, nullptr);
    std::string path = srcString;
    if (srcString != nullptr) {
        env->ReleaseStringUTFChars(srcPath, srcString);
    }
    std::ifstream f;
    f.open(path);
    if (!f.is_open()) {
        return 0;
    }
    f.seekg(0, std::ios::end);
    auto length = f.tellg();
    f.seekg(0, std::ios::beg);
    if (length <= 0) {
        f.close();
        return 0;
    }
    auto *arr = new char[(size_t) length + 1];
    f.read(arr, length);
    f.close();
    arr[length] = '\0';
    bool orig;
    std::string jsonString = doDecompressResource(length, arr, orig);
    if (orig) {
        info.animation = tvg::Animation::gen();
        info.animation->picture()->load(arr, length, "lottie", nullptr, true);
    }
    delete[] arr;
    if (!orig) {
        if (jsonString.empty()) {
            return 0;
        }
        info.animation = tvg::Animation::gen();
        info.animation->picture()->load(jsonString.data(), jsonString.size(), "lottie", nullptr,
                                        true);
    }
    if (!info.animation) {
        return false;
    }

    info.canvas = tvg::SwCanvas::gen();
    if (!info.canvas) {
        return false;
    }
    auto *pixels = new uint32_t[w * h];
    info.canvas->mempool(tvg::SwCanvas::Individual);
    info.canvas->target(pixels, w, w, h,
                        tvg::ColorSpace::ABGR8888S);

    if (!transparent) {
        auto bg = tvg::Shape::gen();
        bg->appendRect(0, 0, (float) w, (float) h);
        bg->fill(((bgColor >> 16) & 0xff), ((bgColor >> 8) & 0xff), (bgColor & 0xff));
        info.canvas->push(bg);
    }

    info.canvas->push(info.animation->picture());

    char const *gifNameString = SafeGetStringUTFChars(env, gifName, nullptr);
    std::string gifNameStr = gifNameString;
    if (gifNameString != nullptr) {
        env->ReleaseStringUTFChars(gifName, gifNameString);
    }

    float scale;
    float shiftX = 0.0f, shiftY = 0.0f;
    float w2, h2;
    info.animation->picture()->size(&w2, &h2);

    if (h >= w) {
        if (w2 >= h2) {
            scale = (float) w / w2;
            shiftY = ((float) h - h2 * scale) * 0.5f;
        } else {
            scale = (float) h / h2;
            shiftX = ((float) w - w2 * scale) * 0.5f;
        }
    } else {
        if (w2 < h2) {
            scale = (float) w / w2;
            shiftY = ((float) h - h2 * scale) * 0.5f;
        } else {
            scale = (float) h / h2;
            shiftX = ((float) w - w2 * scale) * 0.5f;
        }
    }

    info.animation->picture()->scale(scale);
    info.animation->picture()->translate(shiftX, shiftY);

    if (fps > 60) {
        fps = 60;
    } else if (fps <= 0) {
        fps = (info.animation->totalFrame() / info.animation->duration());
    }

    auto delay = (1.0f / (float) fps);
    GifWriter writer;
    if (!gifBegin(&writer, gifNameStr.c_str(), w, h, uint32_t(delay * 100.f))) {
        return false;
    }
    auto duration = info.animation->duration();
    if (listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass clazz = env->GetObjectClass(store_Wlistener);

        jmethodID mth_update = env->GetMethodID(clazz, "onProgress", "(II)V");
        jmethodID mth_start = env->GetMethodID(clazz, "onStarted", "()V");
        jmethodID mth_end = env->GetMethodID(clazz, "onFinished", "()V");

        env->CallVoidMethod(store_Wlistener, mth_start);

        for (auto p = 0.0f; p < duration; p += delay) {
            auto frameNo = info.animation->totalFrame() * (p / duration);
            info.animation->frame(frameNo);
            info.canvas->update();
            if (info.canvas->draw(true) == tvg::Result::Success) {
                info.canvas->sync();
            }
            if (!gifWriteFrame(&writer, reinterpret_cast<uint8_t *>(pixels), w, h,
                               uint32_t(delay * 100.0f), transparent)) {
                break;
            }

            env->CallVoidMethod(store_Wlistener, mth_update, (jint) frameNo,
                                (jint) info.animation->totalFrame());
        }

        env->CallVoidMethod(store_Wlistener, mth_end);
    } else {
        for (auto p = 0.0f; p < duration; p += delay) {
            auto frameNo = info.animation->totalFrame() * (p / duration);
            info.animation->frame(frameNo);
            info.canvas->update(info.animation->picture());
            if (info.canvas->draw(true) == tvg::Result::Success) {
                info.canvas->sync();
            }

            if (!gifWriteFrame(&writer, reinterpret_cast<uint8_t *>(pixels), w, h,
                               uint32_t(delay * 100.0f), transparent)) {
                break;
            }
        }
    }
    if (!gifEnd(&writer)) {
        return false;
    }
    delete[] pixels;
    return true;
}
