#include <jni.h>
#include <android/native_window_jni.h>

#include <cassert>

/**
 * Returns an int array of length 3 that the format, width and height values stored at position
 * 0, 1, 2 correspondingly.
 */
extern "C" JNIEXPORT jintArray JNICALL
Java_dev_ragnarok_fenrir_module_camerax_SurfaceUtilNative_nativeGetSurfaceInfo(JNIEnv *env, jobject,
                                                                               jobject jsurface) {
    // Retrieves surface info via native mothods
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    assert(nativeWindow != nullptr);
    int32_t format = ANativeWindow_getFormat(nativeWindow);
    int32_t width = ANativeWindow_getWidth(nativeWindow);
    int32_t height = ANativeWindow_getHeight(nativeWindow);
    ANativeWindow_release(nativeWindow);

    jintArray resultArray = env->NewIntArray(3);
    jint surfaceInfo[3] = {format, width, height};
    env->SetIntArrayRegion(resultArray, 0, 3, surfaceInfo);

    return resultArray;
}