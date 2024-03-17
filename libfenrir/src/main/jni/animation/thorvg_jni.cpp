#include <jni.h>
#include <android/bitmap.h>
#include <unistd.h>
#include <iostream>
#include <vector>
#include <thread>
#include <map>
#include <utime.h>
#include <thorvg.h>
#include "fenrir_native.h"

static pthread_mutex_t *lockMutex = nullptr;
static std::map<std::string, uint32_t> customColorsTable;

extern std::string doDecompressResource(size_t length, char *bytes, bool &orig);

extern bool fenrirNativeThorVGInited;

void getCustomColor(const std::string &name, uint8_t *r, uint8_t *g, uint8_t *b) {
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
Java_dev_ragnarok_fenrir_module_thorvg_ThorVGRender_registerColorsNative(JNIEnv *env, jobject,
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
Java_dev_ragnarok_fenrir_module_thorvg_ThorVGRender_createBitmapNative(JNIEnv *env, jobject,
                                                                       jlong res, jobject bitmap,
                                                                       jint w,
                                                                       jint h) {
    if (!bitmap) {
        return;
    }
    auto u = ((std::vector<char> *) (intptr_t)
            res);
    auto canvas = tvg::SwCanvas::gen();
    if (!canvas) {
        return;
    }

    auto picture = tvg::Picture::gen();
    bool orig;
    std::string jsonString = doDecompressResource(u->size(), u->data(), orig);
    tvg::Result result = orig ? picture->load((const char *) u->data(), u->size(), "svg", "", false)
                              : picture->load((const char *) jsonString.data(), jsonString.size(),
                                              "svg", "", false);
    if (result != tvg::Result::Success) {
        canvas.release();
        return;
    }
    picture->size((float) w, (float) h);

    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        if (canvas->target((uint32_t *) pixels, w, w, h, tvg::SwCanvas::ABGR8888) !=
            tvg::Result::Success) {
            canvas.release();
            AndroidBitmap_unlockPixels(env, bitmap);
            return;
        }

        canvas->push(std::move(picture));
        canvas->draw();
        canvas->sync();
        canvas->clear(true, false);
        canvas.release();
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}
