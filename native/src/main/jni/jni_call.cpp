#include <cstdio>
#include <ctime>
#include <cmath>
#include <jni.h>
#include <thorvg.h>

bool fenrirNativeThorVGInited = false;

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    srand((unsigned int) time(nullptr));

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    //auto threads = std::thread::hardware_concurrency();
    auto threads = 4;
    if (tvg::Initializer::init(threads, tvg::CanvasEngine::Sw) == tvg::Result::Success) {
        fenrirNativeThorVGInited = true;
    }

    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNI_OnUnload(JavaVM *, void *) {
    tvg::Initializer::term(tvg::CanvasEngine::Sw);
    fenrirNativeThorVGInited = false;
}
