#include <jni.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <thread>
#include <iostream>
#include <list>
#include <filesystem>
#include "fenrir_native.h"

static void
listDirRecursive(JNIEnv *env, const std::string &name, jobject listener, int64_t pointer) {
    for (const auto &entry: std::filesystem::directory_iterator(name)) {
        if (entry.path().filename().string() == "." || entry.path().filename().string() == "..")
            continue;
        if (entry.is_directory()) {
            listDirRecursive(env, entry.path().string(), listener, pointer);
        } else if (entry.is_regular_file()) {
            if (listener != nullptr) {
                jweak dir_Wlistener = env->NewWeakGlobalRef(listener);
                jclass ref = env->GetObjectClass(dir_Wlistener);
                auto method = env->GetMethodID(ref, "onEntry", "(Ljava/lang/String;)V");
                if (ref != nullptr && method != nullptr) {
                    env->CallVoidMethod(dir_Wlistener, method,
                                        env->NewStringUTF(entry.path().string().c_str()));
                }
            } else if (pointer != 0) {
                auto *dt = reinterpret_cast<std::list<std::string> *>(pointer);
                dt->push_back(entry.path().string());
            }
        }
    }
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_FileUtils_listDirRecursiveNative(JNIEnv *env, jobject, jstring dir,
                                                                 jobject listener) {
    char const *dirString = SafeGetStringUTFChars(env, dir, nullptr);
    if (!dirString) {
        return;
    }
    std::string v = dirString;
    env->ReleaseStringUTFChars(dir, dirString);
    listDirRecursive(env, v, listener, 0);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_FileUtils_listDirRecursiveNativePointer(JNIEnv *env, jobject,
                                                                        jstring dir,
                                                                        jlong pointer) {
    char const *dirString = SafeGetStringUTFChars(env, dir, nullptr);
    if (!dirString) {
        return;
    }
    std::string v = dirString;
    env->ReleaseStringUTFChars(dir, dirString);
    listDirRecursive(env, v, nullptr, pointer);
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_FileUtils_getThreadsCountNative(JNIEnv *, jobject) {
    return (jint) std::thread::hardware_concurrency();
}